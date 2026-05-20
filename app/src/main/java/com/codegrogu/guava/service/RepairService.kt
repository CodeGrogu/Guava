package com.codegrogu.guava.service

import com.codegrogu.guava.firebase.FirebaseConfig
import com.codegrogu.guava.model.MechanicNote
import com.codegrogu.guava.model.RepairTask
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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
        val completedAt = docData["completedAt"] as? Long

        // Extract the notes array and convert each one to a MechanicNote object
        // Notes are stored as a List of Maps in Firestore
        val notesList = mutableListOf<MechanicNote>()
        val notesArray = docData["notes"] as? List<Map<String, Any?>> ?: emptyList()

        for (noteMap in notesArray) {
            // Build each mechanic note from the Firestore data
            val note = MechanicNote(
                text = noteMap["text"] as? String ?: "",
                mechanicUid = noteMap["mechanicUid"] as? String ?: "",
                mechanicName = noteMap["mechanicName"] as? String ?: "",
                timestamp = noteMap["timestamp"] as? Long ?: 0L
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
}

