package com.codegrogu.guava.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.codegrogu.guava.model.Vehicle
import com.codegrogu.guava.service.EmployeePerformanceReport
import com.codegrogu.guava.ui.theme.IndustrialBlack
import com.codegrogu.guava.ui.theme.SafetyOrange
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EmployeeReportCard(
    report: EmployeePerformanceReport,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(bottom = 8.dp, end = 8.dp)) {
        // Shadow/Offset effect like other garage components
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(IndustrialBlack.copy(alpha = 0.35f))
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, IndustrialBlack),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = report.mechanicName.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = SafetyOrange
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TOTAL TASKS:",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.totalTasksCompleted.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "VEHICLES WORKED ON:",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (report.workedOnVehicleIds.isEmpty()) {
                    Text(
                        text = "NONE",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(report.workedOnVehicleIds) { id ->
                            Surface(
                                color = IndustrialBlack,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = id.takeLast(6).uppercase(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleReportCard(
    vehicle: Vehicle,
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_VALUE")
    var showFullScreenImage by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dateString = vehicle.checkInTimestamp?.toDate()?.let { dateFormat.format(it) } ?: "N/A"

    if (showFullScreenImage && vehicle.conditionImageUrl.isNotEmpty()) {
        FullScreenImageDialog(
            imageUrl = vehicle.conditionImageUrl,
            onDismiss = { showFullScreenImage = false }
        )
    }

    Box(modifier = modifier.padding(bottom = 8.dp, end = 8.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(IndustrialBlack.copy(alpha = 0.35f))
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, IndustrialBlack.copy(alpha = 0.6f)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .border(1.dp, IndustrialBlack)
                        .clickable { showFullScreenImage = true },
                    color = IndustrialBlack
                ) {
                    if (vehicle.conditionImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = vehicle.conditionImageUrl,
                            contentDescription = "Condition Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text("NO IMG", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vehicle.licensePlate.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "IN: $dateString",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ODOMETER:",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${vehicle.initialKm} KM",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "BY: ${vehicle.checkedInByName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = SafetyOrange,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IndustrialBlack)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full Screen Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(IndustrialBlack.copy(alpha = 0.8f), MaterialTheme.shapes.extraSmall)
                    .border(1.dp, SafetyOrange, MaterialTheme.shapes.extraSmall)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = SafetyOrange)
            }
        }
    }
}
