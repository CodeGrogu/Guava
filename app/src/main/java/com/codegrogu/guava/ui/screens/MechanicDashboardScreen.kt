@file:OptIn(ExperimentalMaterial3Api::class)

package com.codegrogu.guava.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codegrogu.guava.model.Vehicle
import com.codegrogu.guava.service.VehicleService
import com.codegrogu.guava.ui.components.GarageBackground
import com.codegrogu.guava.ui.components.GarageButton
import com.codegrogu.guava.ui.components.GarageImagePreview
import com.codegrogu.guava.ui.theme.SafetyOrange
import kotlinx.coroutines.launch

@Composable
fun MechanicDashboardScreen(
    userName: String,
    onNavigateToCheckIn: () -> Unit,
    onOpenVehicle: (Vehicle) -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var isLoadingVehicles by remember { mutableStateOf(true) }
    var vehicleError by remember { mutableStateOf<String?>(null) }

    fun loadVehicles() {
        scope.launch {
            isLoadingVehicles = true
            vehicleError = null

            VehicleService.getCheckedInVehicles()
                .onSuccess { vehicles = it }
                .onFailure { vehicleError = it.message ?: "Unable to load checked-in vehicles." }

            isLoadingVehicles = false
        }
    }

    LaunchedEffect(Unit) {
        loadVehicles()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GarageBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "MECHANIC DASHBOARD",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "Welcome $userName",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )

            GarageButton(
                text = "Vehicle Check-In",
                onClick = onNavigateToCheckIn,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHECKED-IN VEHICLES",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { loadVehicles() }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh vehicles",
                        tint = SafetyOrange
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                when {
                    isLoadingVehicles -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SafetyOrange)
                        }
                    }

                    vehicleError != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = vehicleError ?: "",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    vehicles.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NO VEHICLES CHECKED IN",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = vehicles,
                                key = { vehicle -> vehicle.vehicleId }
                            ) { vehicle ->
                                VehicleRow(
                                    vehicle = vehicle,
                                    onOpenVehicle = { onOpenVehicle(vehicle) }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = SolidColor(SafetyOrange)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = SafetyOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOG OUT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SafetyOrange,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun VehicleRow(
    vehicle: Vehicle,
    onOpenVehicle: () -> Unit
) {
    Surface(
        onClick = onOpenVehicle,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val previewImage = vehicle.conditionImageUrls.firstOrNull()
                ?: vehicle.conditionImageUrl

            if (previewImage.isNotBlank()) {
                GarageImagePreview(
                    imageUrl = previewImage,
                    contentDescription = "Vehicle condition photo",
                    modifier = Modifier.size(64.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = SafetyOrange,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vehicle.licensePlate.ifBlank { "UNKNOWN VEHICLE" },
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "${vehicle.initialKm} km • checked in by ${vehicle.checkedInByName.ifBlank { "Unknown" }}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Icon(
                imageVector = Icons.Filled.Build,
                contentDescription = null,
                tint = SafetyOrange
            )
        }
    }
}
