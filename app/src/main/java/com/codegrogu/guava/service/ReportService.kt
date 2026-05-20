package com.codegrogu.guava.service

import com.codegrogu.guava.firebase.FirebaseConfig
import com.codegrogu.guava.model.DateRange
import com.codegrogu.guava.model.Vehicle
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

/**
 * Sanitize and format data for the Manager UI.
 */
data class EmployeePerformanceReport(
    val mechanicId: String,
    val mechanicName: String,
    val totalTasksCompleted: Int,
    val workedOnVehicleIds: List<String>
)

object ReportService {

    /**
     * Query all tasks completed/noted by a specific user within a date range.
     * Uses collectionGroup to search across all 'tasks' sub-collections.
     */
    suspend fun getEmployeePerformanceReport(
        mechanicId: String,
        mechanicName: String,
        dateRange: DateRange
    ): Result<EmployeePerformanceReport> {
        return try {
            val startTimestamp = Timestamp(dateRange.start)
            val endTimestamp = Timestamp(dateRange.end)

            val snapshot = FirebaseConfig.firestore
                .collectionGroup("tasks")
                .whereEqualTo("completedByUid", mechanicId)
                .whereGreaterThanOrEqualTo("completedAt", startTimestamp)
                .whereLessThanOrEqualTo("completedAt", endTimestamp)
                .get()
                .await()

            // Extract unique vehicle IDs from the task document paths
            // Path: /vehicles/{vehicleId}/tasks/{taskId}
            val vehicleIds = snapshot.documents.mapNotNull { doc ->
                doc.reference.parent.parent?.id
            }.distinct()

            Result.success(
                EmployeePerformanceReport(
                    mechanicId = mechanicId,
                    mechanicName = mechanicName,
                    totalTasksCompleted = snapshot.size(),
                    workedOnVehicleIds = vehicleIds
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
                Vehicle(
                    vehicleId = doc.id,
                    licensePlate = doc.getString("licensePlate") ?: "UNKNOWN",
                    initialKm = (doc.getLong("initialKm") ?: 0L).toInt(),
                    conditionImageUrl = doc.getString("conditionImageUrl") ?: "",
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
}
