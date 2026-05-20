package com.codegrogu.guava.service

import android.net.Uri
import com.codegrogu.guava.firebase.FirebaseConfig
import com.codegrogu.guava.model.Vehicle
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import java.io.File
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
            validateLocalImage(compressedImageUri)

            val storageRef = FirebaseConfig
                .storage
                .reference
                .child("condition_photos")
                .child("${UUID.randomUUID()}.jpg")

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            // Upload image
            val uploadSnapshot = storageRef
                .putFile(compressedImageUri, metadata)
                .await()

            // Fetch download URL
            val downloadUrl = try {
                uploadSnapshot.storage.downloadUrl.await().toString()
            } catch (error: StorageException) {
                if (error.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) {
                    throw error
                }

                delay(250)
                uploadSnapshot.storage.downloadUrl.await().toString()
            }

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

            val vehicleRef = FirebaseConfig
                .firestore
                .collection("vehicles")
                .document()

            val initTaskRef = vehicleRef
                .collection("tasks")
                .document("_init")

            val batch = FirebaseConfig.firestore.batch()
            batch.set(vehicleRef, vehicleData)
            batch.set(initTaskRef, mapOf("initialized" to true))
            batch.commit()
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

    private fun validateLocalImage(uri: Uri) {
        if (uri.scheme != "file") {
            return
        }

        val path = uri.path
            ?: throw IllegalArgumentException("Captured image path is missing.")

        if (!File(path).exists()) {
            throw IllegalArgumentException("Captured image file does not exist.")
        }
    }
}
