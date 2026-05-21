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

    private fun convertFirestoreDocToRepairTask(
        docId: String,
        docData: Map<String, Any?>
    ): RepairTask {
        val description = docData["description"] as? String ?: ""
        val isCompleted = docData["isCompleted"] as? Boolean ?: false
        val completedByUid = docData["completedByUid"] as? String ?: ""
        val completedByName = docData["completedByName"] as? String ?: ""
        val completedAt = toMillis(docData["completedAt"])

        val notesArray = (docData["notes"] as? List<*>)
            ?.mapNotNull { it as? Map<*, *> }
            ?: emptyList()

        val notes = notesArray.map { noteMap ->
            MechanicNote(
                text = noteMap["text"] as? String ?: "",
                mechanicUid = noteMap["mechanicUid"] as? String ?: "",
                mechanicName = noteMap["mechanicName"] as? String ?: "",
                timestamp = toMillis(noteMap["timestamp"]) ?: 0L,
                imageUrl = noteMap["imageUrl"] as? String ?: ""
            )
        }

        return RepairTask(
            id = docId,
            description = description,
            isCompleted = isCompleted,
            completedByUid = completedByUid,
            completedByName = completedByName,
            completedAt = completedAt,
            notes = notes
        )
    }

    fun subscribeToVehicleTasks(vehicleId: String): Flow<List<RepairTask>> {
        return callbackFlow {
            val tasksRef = FirebaseConfig.firestore
                .collection("vehicles")
                .document(vehicleId)
                .collection("tasks")

            val listener = tasksRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val tasks = snapshot
                    ?.documents
                    .orEmpty()
                    .mapNotNull { doc ->
                        if (doc.id == "_init") return@mapNotNull null

                        runCatching {
                            convertFirestoreDocToRepairTask(doc.id, doc.data ?: emptyMap())
                        }.getOrNull()
                    }

                trySend(tasks).isSuccess
            }

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

    suspend fun toggleTaskCompletion(
        vehicleId: String,
        taskId: String,
        isComplete: Boolean,
        mechanicUid: String,
        mechanicName: String
    ): Result<Unit> {
        return try {
            val updateData = if (isComplete) {
                mapOf(
                    "isCompleted" to true,
                    "completedByUid" to mechanicUid,
                    "completedByName" to mechanicName,
                    "completedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf(
                    "isCompleted" to false,
                    "completedByUid" to "",
                    "completedByName" to "",
                    "completedAt" to null
                )
            }

            FirebaseConfig.firestore
                .collection("vehicles")
                .document(vehicleId)
                .collection("tasks")
                .document(taskId)
                .update(updateData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addNoteToTask(
        vehicleId: String,
        taskId: String,
        noteText: String,
        noteImageUrl: String?,
        mechanicUid: String,
        mechanicName: String,
        taskDescription: String = ""
    ): Result<Unit> {
        return try {
            val cleanedText = noteText.trim()
            val cleanedImageUrl = noteImageUrl?.trim().orEmpty()
            val cleanedTaskDescription = taskDescription.trim()

            if (cleanedText.isBlank() && cleanedImageUrl.isBlank()) {
                return Result.failure(IllegalArgumentException("Add a note or attach a photo."))
            }

            val newNote = mutableMapOf<String, Any>(
                "text" to cleanedText,
                "mechanicUid" to mechanicUid,
                "mechanicName" to mechanicName,
                "timestamp" to System.currentTimeMillis()
            )

            if (cleanedImageUrl.isNotBlank()) {
                newNote["imageUrl"] = cleanedImageUrl
            }

            val taskRef = FirebaseConfig.firestore
                .collection("vehicles")
                .document(vehicleId)
                .collection("tasks")
                .document(taskId)

            taskRef
                .update("notes", FieldValue.arrayUnion(newNote))
                .await()

            // Keep a query-friendly note index without changing the task document shape.
            taskRef
                .collection("notes")
                .add(
                    buildMap<String, Any> {
                        put("vehicleId", vehicleId)
                        put("taskId", taskId)
                        put("taskDescription", cleanedTaskDescription)
                        put("noteText", cleanedText)
                        put("mechanicUid", mechanicUid)
                        put("mechanicName", mechanicName)
                        put("timestamp", FieldValue.serverTimestamp())

                        if (cleanedImageUrl.isNotBlank()) {
                            put("imageUrl", cleanedImageUrl)
                        }
                    }
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
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
