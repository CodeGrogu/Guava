package com.codegrogu.guava.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.codegrogu.guava.model.Vehicle
import com.codegrogu.guava.service.EmployeePerformanceReport
import com.codegrogu.guava.ui.theme.IndustrialBlack
import com.codegrogu.guava.ui.theme.SafetyOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmployeeReportCard(
    report: EmployeePerformanceReport,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

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

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NOTES ADDED:",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.totalNotesAdded.toString(),
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "WORK DETAILS:",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (report.workItems.isEmpty()) {
                    Text(
                        text = "NO RECORDED WORK FOR THIS RANGE",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        report.workItems.take(5).forEach { item ->
                            val timeText = item.timestampMillis
                                ?.let { dateFormat.format(Date(it)) }
                                ?: "TIME N/A"

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, IndustrialBlack.copy(alpha = 0.25f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "${item.actionLabel.uppercase()} • ${item.vehicleLabel.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = SafetyOrange
                                )
                                Text(
                                    text = item.taskDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (item.noteText.isNotBlank()) {
                                    Text(
                                        text = "NOTE: ${item.noteText}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                                if (item.imageUrl.isNotBlank()) {
                                    Text(
                                        text = "PHOTO ATTACHED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                    )
                                }
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )
                            }
                        }

                        if (report.workItems.size > 5) {
                            Text(
                                text = "+${report.workItems.size - 5} MORE ITEMS",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
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
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    val imageUrls = vehicle.conditionImageUrls.ifEmpty {
        listOfNotNull(vehicle.conditionImageUrl.takeIf { it.isNotBlank() })
    }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dateString = vehicle.checkInTimestamp?.toDate()?.let { dateFormat.format(it) } ?: "N/A"

    selectedImageUrl?.let { imageUrl ->
        FullScreenImageDialog(
            imageUrl = imageUrl,
            onDismiss = { selectedImageUrl = null }
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
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Primary thumbnail
                    Surface(
                        modifier = Modifier
                            .size(80.dp)
                            .border(1.dp, IndustrialBlack)
                            .clickable(enabled = imageUrls.isNotEmpty()) {
                                selectedImageUrl = imageUrls.first()
                            },
                        color = IndustrialBlack
                    ) {
                        if (imageUrls.isNotEmpty()) {
                            GarageImagePreview(
                                imageUrl = imageUrls.first(),
                                contentDescription = "Condition Photo",
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
                            text = "PHOTOS: ${imageUrls.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Text(
                            text = "BY: ${vehicle.checkedInByName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = SafetyOrange,
                            fontSize = 10.sp
                        )
                    }
                }

                if (imageUrls.size > 1) {
                    Text(
                        text = "CONDITION PHOTOS:",
                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(imageUrls) { imageUrl ->
                            GarageImagePreview(
                                imageUrl = imageUrl,
                                contentDescription = "Condition Photo",
                                modifier = Modifier
                                    .size(width = 72.dp, height = 54.dp)
                                    .clickable { selectedImageUrl = imageUrl }
                            )
                        }
                    }
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
            GarageImagePreview(
                imageUrl = imageUrl,
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
