package com.codegrogu.guava.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codegrogu.guava.model.Vehicle
import com.codegrogu.guava.service.VehicleService
import com.codegrogu.guava.ui.components.CameraCapture
import com.codegrogu.guava.ui.components.GarageBackground
import com.codegrogu.guava.ui.components.GarageButton
import com.codegrogu.guava.ui.components.GarageImagePreview
import com.codegrogu.guava.ui.components.GarageTextField
import com.codegrogu.guava.ui.theme.SafetyOrange
import com.codegrogu.guava.viewmodel.AppViewModel
import kotlinx.coroutines.launch

private const val MAX_CHECK_IN_PHOTOS = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    viewModel: AppViewModel,
    onCheckInComplete: () -> Unit,
    onNavigateBack: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    // Form state
    var licensePlate by remember { mutableStateOf("") }
    var initialKm by remember { mutableStateOf("") }
    val capturedImageUris = remember { mutableStateListOf<Uri>() }
    var cameraSessionKey by remember { mutableIntStateOf(0) }

    // UI state
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        GarageBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {

                TopAppBar(
                    title = {

                        Column {

                            Text(
                                text = "VEHICLE CHECK-IN",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "Log new truck arrival",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    },

                    navigationIcon = {

                        IconButton(
                            onClick = onNavigateBack
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = SafetyOrange
                            )
                        }
                    },

                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Spacer(modifier = Modifier.height(8.dp))

                // License Plate
                GarageTextField(
                    value = licensePlate,
                    onValueChange = {
                        licensePlate = it.uppercase()
                    },
                    label = "Truck License Plate / ID",
                    leadingIcon = Icons.Default.DirectionsCar,
                    keyboardOptions = KeyboardOptions.Default
                )

                // Odometer
                GarageTextField(
                    value = initialKm,
                    onValueChange = { value ->

                        if (value.all { it.isDigit() }) {
                            initialKm = value
                        }
                    },
                    label = "Odometer Reading (km)",
                    leadingIcon = Icons.Default.Speed,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                // Camera
                key(cameraSessionKey) {
                    CameraCapture(
                        onImageCaptured = { uri ->
                            if (capturedImageUris.size < MAX_CHECK_IN_PHOTOS) {
                                capturedImageUris.add(uri)
                            } else {
                                errorMessage = "Maximum $MAX_CHECK_IN_PHOTOS condition photos allowed."
                            }
                            cameraSessionKey += 1
                        },
                        onImageCleared = {}
                    )
                }

                if (capturedImageUris.isNotEmpty()) {
                    Text(
                        text = "${capturedImageUris.size} CONDITION PHOTO${if (capturedImageUris.size == 1) "" else "S"} CAPTURED",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )

                    Text(
                        text = "Maximum $MAX_CHECK_IN_PHOTOS photos while Firebase Storage is unavailable.",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(capturedImageUris, key = { it.toString() }) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                            ) {
                                GarageImagePreview(
                                    imageUrl = uri.toString(),
                                    contentDescription = "Captured condition photo",
                                    modifier = Modifier.fillMaxSize()
                                )

                                IconButton(
                                    onClick = { capturedImageUris.remove(uri) },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove photo",
                                        tint = SafetyOrange
                                    )
                                }
                            }
                        }
                    }
                }

                // Error banner
                errorMessage?.let { message ->

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {

                        Text(
                            text = "⚠ $message",
                            modifier = Modifier.padding(12.dp),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Submit button
                GarageButton(
                    text = "SUBMIT CHECK-IN",
                    isLoading = isSubmitting,
                    enabled = !isSubmitting,
                    onClick = {
                        val currentUser = uiState.currentUser

                        when {

                            licensePlate.isBlank() -> {
                                errorMessage = "License plate / ID is required."
                                return@GarageButton
                            }

                            initialKm.isBlank() -> {
                                errorMessage = "Odometer reading is required."
                                return@GarageButton
                            }

                            initialKm.toIntOrNull() == null -> {
                                errorMessage = "Odometer must be a valid number."
                                return@GarageButton
                            }

                            capturedImageUris.isEmpty() -> {
                                errorMessage = "At least one condition photo is required."
                                return@GarageButton
                            }

                            currentUser == null -> {
                                errorMessage = "Session expired. Please log in again."
                                return@GarageButton
                            }
                        }

                        errorMessage = null
                        isSubmitting = true

                        scope.launch {

                            val imageUrls = mutableListOf<String>()

                            for (imageUri in capturedImageUris) {
                                val uploadResult = VehicleService.uploadConditionImage(imageUri)

                                if (uploadResult.isFailure) {
                                    errorMessage =
                                        "Image upload failed: ${
                                            uploadResult.exceptionOrNull()?.message
                                                ?: "Unknown error"
                                        }"

                                    isSubmitting = false
                                    return@launch
                                }

                                imageUrls.add(uploadResult.getOrThrow())
                            }

                            // Create vehicle object
                            val vehicle = Vehicle(
                                licensePlate = licensePlate.trim(),
                                initialKm = initialKm.toInt(),
                                conditionImageUrl = imageUrls.first(),
                                conditionImageUrls = imageUrls
                            )

                            // Save check-in
                            val checkInResult =
                                VehicleService.createCheckInRecord(
                                    vehicle = vehicle,
                                    currentUserUid = currentUser.id,
                                    currentUserName = currentUser.name
                                )

                            isSubmitting = false

                            checkInResult
                                .onSuccess {

                                    Toast.makeText(
                                        context,
                                        "✓ ${licensePlate.trim()} checked in successfully",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    onCheckInComplete()
                                }

                                .onFailure { error ->

                                    errorMessage =
                                        "Check-in failed: ${error.message}"
                                }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
