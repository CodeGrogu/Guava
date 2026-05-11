package com.codegrogu.guava.service

import android.net.Uri
import com.codegrogu.guava.firebase.FirebaseConfig
import com.codegrogu.guava.model.Vehicle
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

object VehicleService {

    // ─────────────────────────────────────────────────────────
    // Upload vehicle condition image to Firebase Storage
    // Returns the download URL
    // ─────────────────────────────────────────────────────────
    suspend fun uploadConditionImage(
        compressedImageUri: Uri
    ): Result<String> {

        return try {

            val fileName = "condition_photos/${UUID.randomUUID()}.jpg"

            val storageRef = FirebaseConfig
                .storage
                .reference
                .child(fileName)

            // Upload image
            storageRef.putFile(compressedImageUri).await()

            // Fetch download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()

            Result.success(downloadUrl)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────
    // Create a new vehicle check-in record
    // ─────────────────────────────────────────────────────────
    suspend fun createCheckInRecord(
        vehicle: Vehicle,
        currentUserUid: String,
        currentUserName: String
    ): Result<String> {

        return try {

            val vehicleData = hashMapOf(
                "licensePlate" to vehicle.licensePlate,
                "initialKm" to vehicle.initialKm,
                "conditionImageUrl" to vehicle.conditionImageUrl,
                "checkedInByUid" to currentUserUid,
                "checkedInByName" to currentUserName,
                "checkInTimestamp" to FieldValue.serverTimestamp(),
                "status" to "checked_in"
            )

            // Create vehicle document
            val vehicleRef = FirebaseConfig
                .firestore
                .collection("vehicles")
                .add(vehicleData)
                .await()

            // Create placeholder tasks document
            FirebaseConfig
                .firestore
                .collection("vehicles")
                .document(vehicleRef.id)
                .collection("tasks")
                .document("_init")
                .set(
                    mapOf(
                        "initialized" to true
                    )
                )
                .await()

            Result.success(vehicleRef.id)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────
    // Fetch all checked-in vehicles
    // ─────────────────────────────────────────────────────────
    suspend fun getCheckedInVehicles(): Result<List<Vehicle>> {

        return try {

            val snapshot = FirebaseConfig
                .firestore
                .collection("vehicles")
                .whereEqualTo("status", "checked_in")
                .orderBy(
                    "checkInTimestamp",
                    Query.Direction.DESCENDING
                )
                .get()
                .await()

            val vehicles = snapshot.documents.map { doc ->

                Vehicle(
                    vehicleId = doc.id,
                    licensePlate = doc.getString("licensePlate") ?: "",
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