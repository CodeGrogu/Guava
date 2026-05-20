package com.codegrogu.guava.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.codegrogu.guava.firebase.FirebaseConfig
import com.codegrogu.guava.model.Vehicle
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

object VehicleService {
    private const val TAG = "VehicleService"
    private const val MAX_INLINE_IMAGE_BYTES = 180_000

    // ─────────────────────────────────────────────────────────
    // Upload vehicle condition image to Firebase Storage
    // Returns the download URL
    // ─────────────────────────────────────────────────────────
    suspend fun uploadConditionImage(
        compressedImageUri: Uri
    ): Result<String> {

        return try {
            val imageBytes = readLocalImageBytes(compressedImageUri)
            Result.success(uploadToStorageOrInline(compressedImageUri, imageBytes))

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
                "conditionImageUrls" to vehicle.conditionImageUrls.ifEmpty {
                    listOfNotNull(vehicle.conditionImageUrl.takeIf { it.isNotBlank() })
                },
                "checkedInByUid" to currentUserUid,
                "checkedInByName" to currentUserName,
                "checkInTimestamp" to FieldValue.serverTimestamp(),
                "status" to "checked_in"
            )

            val vehicleRef = FirebaseConfig
                .firestore
                .collection("vehicles")
                .document()

            vehicleRef
                .set(vehicleData)
                .await()

            Result.success(vehicleRef.id)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────
    // Fetch all checked-in vehicles
    // ─────────────────────────────────────────────────────────
    suspend fun getVehicle(vehicleId: String): Result<Vehicle> {
        return try {
            val doc = FirebaseConfig
                .firestore
                .collection("vehicles")
                .document(vehicleId)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.failure(IllegalArgumentException("Vehicle not found."))
            }

            Result.success(documentToVehicle(doc))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCheckedInVehicles(): Result<List<Vehicle>> {

        return try {

            val snapshot = FirebaseConfig
                .firestore
                .collection("vehicles")
                .whereEqualTo("status", "checked_in")
                .get()
                .await()

            val vehicles = snapshot.documents.map { doc -> documentToVehicle(doc) }

            Result.success(
                vehicles.sortedByDescending { vehicle ->
                    vehicle.checkInTimestamp?.toDate()?.time ?: 0L
                }
            )

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    private fun documentToVehicle(doc: DocumentSnapshot): Vehicle {
        return Vehicle(
            vehicleId = doc.id,
            licensePlate = doc.getString("licensePlate") ?: "",
            initialKm = (doc.getLong("initialKm") ?: 0L).toInt(),
            conditionImageUrl = doc.getString("conditionImageUrl") ?: "",
            conditionImageUrls = (doc.get("conditionImageUrls") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: listOfNotNull(doc.getString("conditionImageUrl")?.takeIf { it.isNotBlank() }),
            checkedInByUid = doc.getString("checkedInByUid") ?: "",
            checkedInByName = doc.getString("checkedInByName") ?: "",
            checkInTimestamp = doc.getTimestamp("checkInTimestamp")
        )
    }

    private fun readLocalImageBytes(uri: Uri): ByteArray? {
        if (uri.scheme != "file") {
            return null
        }

        val path = uri.path
            ?: throw IllegalArgumentException("Captured image path is missing.")

        val imageFile = File(path)
        if (!imageFile.exists()) {
            throw IllegalArgumentException("Captured image file does not exist.")
        }

        if (imageFile.length() == 0L) {
            throw IllegalArgumentException("Captured image file is empty.")
        }

        return imageFile.readBytes()
    }

    private suspend fun uploadToStorageOrInline(uri: Uri, localBytes: ByteArray?): String {
        val storageRef = FirebaseConfig.storage
            .reference
            .child("condition_photos")
            .child("${UUID.randomUUID()}.jpg")

        return try {
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            if (localBytes != null) {
                storageRef.putBytes(localBytes, metadata).await()
            } else {
                storageRef.putFile(uri, metadata).await()
            }

            storageRef.downloadUrl.await().toString()
        } catch (storageError: Exception) {
            Log.w(TAG, "Firebase Storage upload failed; saving bounded inline image.", storageError)
            createInlineImageReference(localBytes)
        }
    }

    private fun createInlineImageReference(imageBytes: ByteArray?): String {
        val bytes = imageBytes
            ?: throw IllegalArgumentException("Inline image fallback requires a local image file.")

        val boundedBytes = if (bytes.size <= MAX_INLINE_IMAGE_BYTES) {
            bytes
        } else {
            compressForInlineStorage(bytes)
        }

        if (boundedBytes.size > MAX_INLINE_IMAGE_BYTES) {
            throw IllegalStateException(
                "Firebase Storage is unavailable and the captured photo is too large for Firestore fallback."
            )
        }

        val encodedImage = Base64.encodeToString(boundedBytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$encodedImage"
    }

    private fun compressForInlineStorage(imageBytes: ByteArray): ByteArray {
        val originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return imageBytes

        val scale = minOf(1f, 640f / originalBitmap.width, 360f / originalBitmap.height)
        val bitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            (originalBitmap.width * scale).toInt().coerceAtLeast(1),
            (originalBitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )

        val qualities = listOf(45, 35, 25)
        val compressedBytes = qualities
            .asSequence()
            .map { quality ->
                ByteArrayOutputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                    output.toByteArray()
                }
            }
            .firstOrNull { it.size <= MAX_INLINE_IMAGE_BYTES }
            ?: ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 20, output)
                output.toByteArray()
            }

        if (bitmap !== originalBitmap) {
            bitmap.recycle()
        }
        originalBitmap.recycle()

        return compressedBytes
    }

}
