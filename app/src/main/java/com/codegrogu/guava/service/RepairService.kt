package com.codegrogu.guava.service

import com.codegrogu.guava.firebase.FirebaseConfig
import com.codegrogu.guava.model.MechanicNote
import com.codegrogu.guava.model.RepairTask
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object RepairService {

    // ─────────────────────────────────────────────────────────────────────────────
    // HELPER FUNCTION: Convert a Firestore document into a RepairTask object
    // This is needed because Firestore returns raw data, but we need our Kotlin model.
    // ─────────────────────────────────────────────────────────────────────────────
    private fun convertFirestoreDocToRepairTask(
        docId: String,
        docData: Map<String, Any?>
    ): RepairTask {
        // Extract basic task information from the Firestore document
        val description = docData["description"] as? String ?: ""
        val isCompleted = docData["isCompleted"] as? Boolean ?: false
        val completedByUid = docData["completedByUid"] as? String ?: ""
        val completedByName = docData["completedByName"] as? String ?: ""
        val completedAt = toMillis(docData["completedAt"])

        // Extract the notes array and convert each one to a MechanicNote object
        // Notes are stored as a List of Maps in Firestore
        val notesList = mutableListOf<MechanicNote>()
        val notesArray = (docData["notes"] as? List<*>)
            ?.mapNotNull { it as? Map<*, *> }
            ?: emptyList()

        for (noteMap in notesArray) {
            // Build each mechanic note from the Firestore data
            val note = MechanicNote(
                text = noteMap["text"] as? String ?: "",
                mechanicUid = noteMap["mechanicUid"] as? String ?: "",
                mechanicName = noteMap["mechanicName"] as? String ?: "",
                timestamp = toMillis(noteMap["timestamp"]) ?: 0L,
                imageUrl = noteMap["imageUrl"] as? String ?: ""
            )
            notesList.add(note)
        }

        // Return the complete RepairTask object with all data properly converted
        return RepairTask(
            id = docId,
            description = description,
            isCompleted = isCompleted,
            completedByUid = completedByUid,
            completedByName = completedByName,
            completedAt = completedAt,
            notes = notesList
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // FR-3.1:
    // Listen to all repair tasks for a specific vehicle in REAL-TIME.
    // Returns a FLOW — think of it as a stream of data updates from Firestore.
    //
    // What is a Flow?
    // A Flow is like opening a fire hose of data. Every time a task changes in
    // Firestore (someone completes a task, adds a note, etc.), the Flow pipes
    // that update immediately to subscribers. No manual refresh needed!
    //
    // The composable screens will collect() from this Flow and automatically
    // re-render whenever tasks change.
    // ─────────────────────────────────────────────────────────────────────────────
    fun subscribeToVehicleTasks(vehicleId: String): Flow<List<RepairTask>> {
        // callbackFlow is like a translator: it takes Firestore's push-style updates
        // and converts them into a Flow that Kotlin coroutines can use.
        return callbackFlow {
            // Get a reference to the tasks sub-collection for this vehicle
            val tasksRef = FirebaseConfig.firestore
                .collection("vehicles")
                .document(vehicleId)
                .collection("tasks")

            // Listen for REAL-TIME updates from Firestore
            // This listener will be called immediately with current data,
            // then again every time ANY task changes.
            val listener = tasksRef.addSnapshotListener { snapshot, error ->
                // If something goes wrong (network error, permissions, etc.)
                if (error != null) {
                    // Close the Flow and report the error to subscribers
                    close(error)
                    return@addSnapshotListener
                }

                // If we got here, the snapshot is valid
                if (snapshot != null) {
                    // Convert each Firestore document into a RepairTask object
                    val tasks = snapshot.documents.mapNotNull { doc ->
                        // Skip the "_init" placeholder document that was created
                        // during vehicle check-in (doesn't represent a real task)
                        if (doc.id == "_init") {
                            return@mapNotNull null
                        }

                        // Convert the Firestore doc to a RepairTask
                        try {
                            convertFirestoreDocToRepairTask(doc.id, doc.data ?: emptyMap())
                        } catch (e: Exception) {
                            // If conversion fails, skip this task and log the error
                            e.printStackTrace()
                            null
                        }
                    }

                    // Send the list of tasks through the Flow to subscribers
                    // Subscribers (composables) will immediately see this update
                    trySend(tasks).isSuccess
                }
            }

            // IMPORTANT: Clean up the listener when the Flow is cancelled
            // This stops Firestore from sending updates and saves bandwidth/battery
            awaitClose {
                listener.remove()
            }
        }
    }

    suspend fun addRepairTask(
        vehicleId: String,
        description: String,
        mechanicUid: String,
        mechanicName: String
    ): Result<Unit> {
        return try {
            val cleanedDescription = description.trim()
            if (cleanedDescription.isBlank()) {
                return Result.failure(IllegalArgumentException("Task description is required."))
            }

            FirebaseConfig.firestore
                .collection("vehicles")
                .document(vehicleId)
                .collection("tasks")
                .add(
                    mapOf(
                        "description" to cleanedDescription,
                        "isCompleted" to false,
                        "completedByUid" to "",
                        "completedByName" to "",
                        "completedAt" to null,
                        "notes" to emptyList<Map<String, Any>>(),
                        "createdByUid" to mechanicUid,
                        "createdByName" to mechanicName,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // FR-3.2 / NFR-1 (Accountability):
    // Toggle a repair task's completion status (mark done or undo).
    // Includes mechanic attribution so we know WHO completed it and WHEN.
    //
    // Parameters:
    //   vehicleId:      The vehicle the task belongs to
    //   taskId:         The specific task to toggle
    //   isComplete:     true to mark done, false to undo completion
    //   mechanicUid:    The Firebase Auth UID of the mechanic making this change
    //   mechanicName:   The display name of the mechanic (for UI readability)
    //
    // Returns Result<Unit>: Success if the update worked, Failure with error if not
    // ─────────────────────────────────────────────────────────────────────────────
    suspend fun toggleTaskCompletion(
        vehicleId: String,
        taskId: String,
        isComplete: Boolean,
        mechanicUid: String,
        mechanicName: String
    ): Result<Unit> {
        return try {
            // Prepare the data to send to Firestore
            // If marking complete: set who did it and when
            // If marking incomplete: clear who did it (undo)
            val updateData = if (isComplete) {
                // Mark as complete with mechanic details
                mapOf(
                    "isCompleted" to true,
                    "completedByUid" to mechanicUid,
                    "completedByName" to mechanicName,
                    // Server timestamp ensures accuracy even if device clock is wrong
                    "completedAt" to FieldValue.serverTimestamp()
                )
            } else {
                // Mark as incomplete (undo) by clearing completion info
                mapOf(
                    "isCompleted" to false,
                    "completedByUid" to "",
                    "completedByName" to "",
                    "completedAt" to null
                )
            }

            // Get a reference to the specific task document and update it
            FirebaseConfig.firestore
                .collection("vehicles")
                .document(vehicleId)
                .collection("tasks")
                .document(taskId)
                .update(updateData)
                .await()

            // Success! Return an empty Result to indicate the operation worked
            Result.success(Unit)
        } catch (e: Exception) {
            // Something went wrong (network error, permissions, task not found, etc.)
            // Return the error so the caller can handle it (show snackbar, etc)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // FR-3.3 / NFR-1 (Accountability):
    // Add a mechanic's note to a repair task.
    // Each note is tagged with who wrote it, creating an audit trail.
    //
    // How it works:
    // The "notes" field in Firestore is an ARRAY. Each note is a Map containing:
    //   {text, mechanicUid, mechanicName, timestamp}
    // When a mechanic adds a note, we APPEND it to this array.
    //
    // Parameters:
    //   vehicleId:      The vehicle the task belongs to
    //   taskId:         The specific task to add a note to
    //   noteText:       The actual note text the mechanic wrote
    //   mechanicUid:    The Firebase Auth UID of the mechanic writing the note
    //   mechanicName:   The display name of the mechanic (for UI readability)
    //
    // Returns Result<Unit>: Success if the note was added, Failure with error if not
    // ────────────────────────────────────��────────────────────────────────────────
    suspend fun addNoteToTask(
        vehicleId: String,
        taskId: String,
        noteText: String,
        mechanicUid: String,
        mechanicName: String
    ): Result<Unit> {
        return try {
            // Create the note object as a Map (this is what gets stored in Firestore)
            val newNote = mapOf(
                "text" to noteText,
                "mechanicUid" to mechanicUid,
                "mechanicName" to mechanicName,
                // Exact timestamp when the note is created on the server (accurate)
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            // Get a reference to the task document
            // Use arrayUnion() to APPEND the note to the existing notes array
            // (This is a Firestore helper that safely adds without overwriting)
            FirebaseConfig.firestore
                .collection("vehicles")
                .document(vehicleId)
                .collection("tasks")
                .document(taskId)
                .update("notes", FieldValue.arrayUnion(newNote))
                .await()

            // Success! Return an empty Result to indicate the note was added
            Result.success(Unit)
        } catch (e: Exception) {
            // Something went wrong (network error, permissions, task not found, etc.)
            // Return the error so the caller can handle it (show snackbar, etc)
            Result.failure(e)
        }
    }

    private fun toMillis(value: Any?): Long? {
        return when (value) {
            is Long -> value
            is Number -> value.toLong()
            is Timestamp -> value.toDate().time
            else -> null
        }
    }
}
