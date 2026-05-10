package com.codegrogu.guava.service

import android.net.Uri
import com.codegrogu.guava.firebase.FirebaseConfig
import com.codegrogu.guava.model.Vehicle
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.util.UUID

object VehicleService {

    // ─────────────────────────────────────────────────────────
    // FR-1.2 / NFR-5:
    // Uploads a compressed image to Firebase Storage.
    // Returns the public download URL string.
    // ─────────────────────────────────────────────────────────
    suspend fun uploadConditionImage(compressedImageUri: Uri): Result<String> {
        return try {
            val fileName  = "condition_photos/${UUID.randomUUID()}.jpg"
            val storageRef = FirebaseConfig.storage.reference.child(fileName)

            storageRef.putFile(compressedImageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────
    // FR-1.1 / FR-1.3:
    // Creates a new vehicle check-in record in Firestore.
    // Also creates an empty "tasks" sub-collection placeholder
    // so Member 3's RepairService can attach tasks to it.
    // ─────────────────────────────────────────────────────────
    suspend fun createCheckInRecord(
        vehicle: Vehicle,
        currentUserUid: String,
        currentUserName: String
    ): Result<String> {
        return try {
            val vehicleData = hashMapOf(
                "licensePlate"      to vehicle.licensePlate,
                "initialKm"         to vehicle.initialKm,
                "conditionImageUrl" to vehicle.conditionImageUrl,
                "checkedInByUid"    to currentUserUid,
                "checkedInByName"   to currentUserName,
                // Server timestamp — accurate regardless of device clock (FR-1.3)
                "checkInTimestamp"  to FieldValue.serverTimestamp(),
                "status"            to "checked_in"
            )

            // Create the vehicle document — Firestore auto-generates the ID
            val vehicleRef = FirebaseConfig.firestore
                .collection("vehicles")
                .add(vehicleData)
                .await()

            // Initialize the tasks sub-collection with a placeholder document
            // so Firestore acknowledges the sub-collection exists.
            // Member 3 (RepairService) will add real tasks here.
            FirebaseConfig.firestore
                .collection("vehicles")
                .document(vehicleRef.id)
                .collection("tasks")
                .document("_init")
                .set(mapOf("initialized" to true))
                .await()

            Result.success(vehicleRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────
    // Fetches all currently checked-in vehicles.
    // Used by the Mechanic Dashboard to list vehicles for repair.
    // ─────────────────────────────────────────────────────────
    suspend fun getCheckedInVehicles(): Result<List<Vehicle>> {
        return try {
            val snapshot = FirebaseConfig.firestore
                .collection("vehicles")
                .whereEqualTo("status", "checked_in")
                .orderBy(
                    "checkInTimestamp",
                    com.google.firebase.firestore.Query.Direction.DESCENDING
                )
                .get()
                .await()

            val vehicles = snapshot.documents.map { doc ->
                Vehicle(
                    vehicleId         = doc.id,
                    licensePlate      = doc.getString("licensePlate") ?: "",
                    initialKm         = (doc.getLong("initialKm") ?: 0L).toInt(),
                    conditionImageUrl = doc.getString("conditionImageUrl") ?: "",
                    checkedInByUid    = doc.getString("checkedInByUid") ?: "",
                    checkedInByName   = doc.getString("checkedInByName") ?: "",
                    checkInTimestamp  = doc.getTimestamp("checkInTimestamp")
                )
            }

            Result.success(vehicles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}