package com.codegrogu.guava.service

import com.codegrogu.guava.firebase.FirebaseConfig
import com.codegrogu.guava.model.DateRange
import com.codegrogu.guava.model.User
import com.codegrogu.guava.model.Vehicle
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

/**
 * Sanitize and format data for the Manager UI.
 */
data class EmployeeWorkItem(
    val vehicleId: String,
    val vehicleLabel: String,
    val taskId: String,
    val taskDescription: String,
    val actionLabel: String,
    val noteText: String = "",
    val imageUrl: String = "",
    val timestampMillis: Long? = null
)

data class EmployeePerformanceReport(
    val mechanicId: String,
    val mechanicName: String,
    val totalTasksCompleted: Int,
    val totalNotesAdded: Int,
    val workedOnVehicleIds: List<String>,
    val workItems: List<EmployeeWorkItem>
)

object ReportService {
    suspend fun getMechanics(): Result<List<User>> {
        return try {
            val snapshot = FirebaseConfig.firestore
                .collection("users")
                .whereEqualTo("role", "MECHANIC")
                .get()
                .await()

            Result.success(
                snapshot.documents.map { doc ->
                    User(
                        id = doc.id,
                        name = doc.getString("name") ?: "Unknown",
                        email = doc.getString("email") ?: ""
                    )
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Query all tasks completed/noted by a specific user within a date range.
     * Scans vehicle task sub-collections to avoid Firestore composite-index preconditions.
     */
    suspend fun getEmployeePerformanceReport(
        mechanicId: String,
        mechanicName: String,
        dateRange: DateRange
    ): Result<EmployeePerformanceReport> {
        return try {
            val startTimestamp = Timestamp(dateRange.start)
            val endTimestamp = Timestamp(dateRange.end)

            val vehicleSnapshot = FirebaseConfig.firestore
                .collection("vehicles")
                .get()
                .await()

            val completedItems = mutableListOf<EmployeeWorkItem>()
            val noteItems = mutableListOf<EmployeeWorkItem>()

            for (vehicleDoc in vehicleSnapshot.documents) {
                val vehicleId = vehicleDoc.id
                val vehicleLabel = vehicleDoc.getString("licensePlate")
                    ?.takeIf { it.isNotBlank() }
                    ?: vehicleId

                val taskSnapshot = vehicleDoc.reference
                    .collection("tasks")
                    .get()
                    .await()

                for (taskDoc in taskSnapshot.documents) {
                    if (taskDoc.id == "_init") {
                        continue
                    }

                    val taskDescription = taskDoc.getString("description") ?: "Repair task"

                    if (
                        taskDoc.getString("completedByUid") == mechanicId &&
                        isInRange(taskDoc.get("completedAt"), startTimestamp, endTimestamp)
                    ) {
                        completedItems.add(
                            EmployeeWorkItem(
                                vehicleId = vehicleId,
                                vehicleLabel = vehicleLabel,
                                taskId = taskDoc.id,
                                taskDescription = taskDescription,
                                actionLabel = "Completed task",
                                timestampMillis = toMillis(taskDoc.get("completedAt"))
                            )
                        )
                    }

                    val indexedNoteItems = taskDoc.reference
                        .collection("notes")
                        .get()
                        .await()
                        .documents
                        .filter { noteDoc ->
                            noteDoc.getString("mechanicUid") == mechanicId &&
                                isInRange(noteDoc.get("timestamp"), startTimestamp, endTimestamp)
                        }
                        .map { noteDoc ->
                            noteIndexToWorkItem(
                                doc = noteDoc,
                                fallbackVehicleId = vehicleId,
                                fallbackVehicleLabel = vehicleLabel,
                                fallbackTaskDescription = taskDescription
                            )
                        }

                    if (indexedNoteItems.isNotEmpty()) {
                        noteItems.addAll(indexedNoteItems)
                    } else {
                        noteItems.addAll(
                            taskNotesToWorkItems(
                                taskDoc = taskDoc,
                                mechanicId = mechanicId,
                                vehicleId = vehicleId,
                                vehicleLabel = vehicleLabel,
                                taskDescription = taskDescription,
                                start = startTimestamp,
                                end = endTimestamp
                            )
                        )
                    }
                }
            }

            val workItems = (completedItems + noteItems)
                .sortedByDescending { it.timestampMillis ?: 0L }

            val vehicleIds = workItems
                .map { it.vehicleId }
                .filter { it.isNotBlank() && it != UNKNOWN_VALUE }
                .distinct()

            Result.success(
                EmployeePerformanceReport(
                    mechanicId = mechanicId,
                    mechanicName = mechanicName,
                    totalTasksCompleted = completedItems.size,
                    totalNotesAdded = noteItems.size,
                    workedOnVehicleIds = vehicleIds,
                    workItems = workItems
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pull initial condition images and kilometers logged at check-in.
     */
    suspend fun getVehicleIntakeReport(dateRange: DateRange): Result<List<Vehicle>> {
        return try {
            val startTimestamp = Timestamp(dateRange.start)
            val endTimestamp = Timestamp(dateRange.end)

            val snapshot = FirebaseConfig.firestore
                .collection("vehicles")
                .whereGreaterThanOrEqualTo("checkInTimestamp", startTimestamp)
                .whereLessThanOrEqualTo("checkInTimestamp", endTimestamp)
                .orderBy("checkInTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val vehicles = snapshot.documents.map { doc ->
                val fallbackImageUrl = doc.getString("conditionImageUrl") ?: ""
                val imageUrls = (doc.get("conditionImageUrls") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()

                Vehicle(
                    vehicleId = doc.id,
                    licensePlate = doc.getString("licensePlate") ?: "UNKNOWN",
                    initialKm = (doc.getLong("initialKm") ?: 0L).toInt(),
                    conditionImageUrl = fallbackImageUrl.ifBlank {
                        imageUrls.firstOrNull().orEmpty()
                    },
                    conditionImageUrls = imageUrls.ifEmpty {
                        listOfNotNull(fallbackImageUrl.takeIf { it.isNotBlank() })
                    },
                    checkedInByUid = doc.getString("checkedInByUid") ?: "",
                    checkedInByName = doc.getString("checkedInByName") ?: "",
                    checkInTimestamp = doc.getTimestamp("checkInTimestamp")
                )
            }

            Result.success(vehicles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun noteIndexToWorkItem(
        doc: DocumentSnapshot,
        fallbackVehicleId: String = UNKNOWN_VALUE,
        fallbackVehicleLabel: String = UNKNOWN_VALUE,
        fallbackTaskDescription: String = "Repair task"
    ): EmployeeWorkItem {
        val taskRef = doc.reference.parent.parent
        val vehicleRef = taskRef?.let { vehicleRefFromTaskRef(it) }
        val vehicleId = doc.getString("vehicleId") ?: vehicleRef?.id ?: fallbackVehicleId
        val taskDescription = doc.getString("taskDescription")?.takeIf { it.isNotBlank() }
            ?: taskRef?.get()?.await()?.getString("description")
            ?: fallbackTaskDescription

        return EmployeeWorkItem(
            vehicleId = vehicleId,
            vehicleLabel = getVehicleLabel(vehicleRef).takeUnless { it == UNKNOWN_VALUE }
                ?: fallbackVehicleLabel,
            taskId = doc.getString("taskId") ?: taskRef?.id ?: UNKNOWN_VALUE,
            taskDescription = taskDescription,
            actionLabel = "Added note",
            noteText = doc.getString("noteText") ?: "",
            imageUrl = doc.getString("imageUrl") ?: "",
            timestampMillis = toMillis(doc.get("timestamp"))
        )
    }

    private fun taskNotesToWorkItems(
        taskDoc: DocumentSnapshot,
        mechanicId: String,
        vehicleId: String,
        vehicleLabel: String,
        taskDescription: String,
        start: Timestamp,
        end: Timestamp
    ): List<EmployeeWorkItem> {
        val notes = (taskDoc.get("notes") as? List<*>)
            ?.mapNotNull { it as? Map<*, *> }
            .orEmpty()

        return notes
            .filter { note ->
                note["mechanicUid"] == mechanicId &&
                    isInRange(note["timestamp"], start, end)
            }
            .map { note ->
                EmployeeWorkItem(
                    vehicleId = vehicleId,
                    vehicleLabel = vehicleLabel,
                    taskId = taskDoc.id,
                    taskDescription = taskDescription,
                    actionLabel = "Added note",
                    noteText = note["text"] as? String ?: "",
                    imageUrl = note["imageUrl"] as? String ?: "",
                    timestampMillis = toMillis(note["timestamp"])
                )
            }
    }

    private fun vehicleRefFromTaskRef(taskRef: DocumentReference): DocumentReference? {
        return taskRef.parent.parent
    }

    private suspend fun getVehicleLabel(vehicleRef: DocumentReference?): String {
        if (vehicleRef == null) {
            return UNKNOWN_VALUE
        }

        return try {
            val vehicleDoc = vehicleRef.get().await()
            vehicleDoc.getString("licensePlate")
                ?.takeIf { it.isNotBlank() }
                ?: vehicleRef.id
        } catch (_: Exception) {
            vehicleRef.id
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

    private fun isInRange(value: Any?, start: Timestamp, end: Timestamp): Boolean {
        val millis = toMillis(value) ?: return false
        return millis in start.toDate().time..end.toDate().time
    }

    private const val UNKNOWN_VALUE = "UNKNOWN"
}
