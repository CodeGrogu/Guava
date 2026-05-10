package com.codegrogu.guava.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.codegrogu.guava.ui.components.GarageTextField
import com.codegrogu.guava.ui.theme.SafetyOrange
import com.codegrogu.guava.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    viewModel: AppViewModel,
    onCheckInComplete: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Read current mechanic from global state
    val currentUser by viewModel.currentUser.collectAsState()

    // Form state
    var licensePlate    by remember { mutableStateOf("") }
    var initialKm       by remember { mutableStateOf("") }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Submission state
    var isSubmitting  by remember { mutableStateOf(false) }
    var errorMessage  by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

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
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
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

                // ── License Plate ────────────────────────
                GarageTextField(
                    value = licensePlate,
                    onValueChange = { licensePlate = it.uppercase() },
                    label = "Truck License Plate / ID",
                    leadingIcon = Icons.Filled.DirectionsCar,
                    keyboardOptions = KeyboardOptions.Default
                )

                // ── Initial Kilometers ───────────────────
                GarageTextField(
                    value = initialKm,
                    onValueChange = { km ->
                        // Only allow numeric input
                        if (km.all { it.isDigit() }) initialKm = km
                    },
                    label = "Odometer Reading (km)",
                    leadingIcon = Icons.Filled.Speed,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // ── Camera Capture ───────────────────────
                CameraCapture(
                    onImageCaptured = { uri ->
                        capturedImageUri = uri
                    }
                )

                // ── Validation error ─────────────────────
                errorMessage?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠ $msg",
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Submit Button ────────────────────────
                GarageButton(
                    text = "SUBMIT CHECK-IN",
                    isLoading = isSubmitting,
                    enabled = !isSubmitting,
                    onClick = {
                        // ── Validation (FR-1.1, FR-1.3) ──
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
                                errorMessage = "Odometer reading must be a number."
                                return@GarageButton
                            }
                            capturedImageUri == null -> {
                                errorMessage = "A condition photo is required."
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
                            // Step 1 — Upload compressed image (NFR-5)
                            val uploadResult = VehicleService.uploadConditionImage(
                                capturedImageUri!!
                            )

                            if (uploadResult.isFailure) {
                                errorMessage = "Image upload failed: " +
                                        (uploadResult.exceptionOrNull()?.message ?: "Unknown error")
                                isSubmitting = false
                                return@launch
                            }

                            val imageUrl = uploadResult.getOrThrow()

                            // Step 2 — Save check-in record to Firestore
                            val vehicle = Vehicle(
                                licensePlate      = licensePlate.trim(),
                                initialKm         = initialKm.toInt(),
                                conditionImageUrl = imageUrl
                            )

                            val checkInResult = VehicleService.createCheckInRecord(
                                vehicle         = vehicle,
                                currentUserUid  = currentUser!!.uid,
                                currentUserName = currentUser!!.name
                            )

                            isSubmitting = false

                            checkInResult.onSuccess {
                                Toast.makeText(
                                    context,
                                    "✓ ${licensePlate.trim()} checked in successfully",
                                    Toast.LENGTH_LONG
                                ).show()
                                onCheckInComplete()
                            }.onFailure { e ->
                                errorMessage = "Check-in failed: ${e.message}"
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}