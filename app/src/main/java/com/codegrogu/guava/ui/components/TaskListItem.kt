package com.codegrogu.guava.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codegrogu.guava.model.RepairTask
import com.codegrogu.guava.ui.theme.SafetyOrange

@Composable
fun TaskListItem(
    task: RepairTask,
    onTaskToggle: (isComplete: Boolean) -> Unit,
    onNoteAdded: (noteText: String, imageUri: Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isNotesExpanded = remember { mutableStateOf(false) }
    val noteInputText = remember { mutableStateOf("") }
    val attachedImageUri = remember { mutableStateOf<Uri?>(null) }
    val showNoteCamera = remember { mutableStateOf(false) }
    val noteCameraSessionKey = remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    isNotesExpanded.value = !isNotesExpanded.value
                }
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { newState ->
                    onTaskToggle(newState)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = SafetyOrange,
                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    checkmarkColor = MaterialTheme.colorScheme.background
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (isNotesExpanded.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isNotesExpanded.value) "Collapse notes" else "Expand notes",
                tint = SafetyOrange,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (task.isCompleted && task.completedByName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Completed by: ${task.completedByName}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = SafetyOrange
                ),
                modifier = Modifier.padding(start = 40.dp)  // Align under task description
            )
        }

        if (isNotesExpanded.value) {
            Spacer(modifier = Modifier.height(12.dp))

            if (task.notes.isNotEmpty()) {
                Text(
                    text = "MECHANIC NOTES:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = SafetyOrange
                    ),
                    modifier = Modifier.padding(start = 40.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                for (note in task.notes) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp)
                    ) {
                        Text(
                            text = "— ${note.mechanicName}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )

                        Text(
                            text = note.text,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )

                        if (note.imageUrl.isNotBlank()) {
                            GarageImagePreview(
                                imageUrl = note.imageUrl,
                                contentDescription = "Mechanic note photo",
                                modifier = Modifier
                                    .padding(top = 4.dp, bottom = 8.dp)
                                    .height(140.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = noteInputText.value,
                onValueChange = { noteInputText.value = it },
                label = { Text("Add a note...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(start = 40.dp),
                maxLines = 3,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            attachedImageUri.value?.let { uri ->
                GarageImagePreview(
                    imageUrl = uri.toString(),
                    contentDescription = "Attached note photo",
                    modifier = Modifier
                        .padding(start = 40.dp)
                        .fillMaxWidth()
                        .height(140.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = { showNoteCamera.value = !showNoteCamera.value },
                modifier = Modifier.padding(start = 40.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = SolidColor(SafetyOrange)
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    tint = SafetyOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (attachedImageUri.value == null) "ATTACH PHOTO" else "CHANGE PHOTO",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SafetyOrange,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (showNoteCamera.value) {
                Spacer(modifier = Modifier.height(8.dp))
                key(noteCameraSessionKey.intValue) {
                    CameraCapture(
                        onImageCaptured = { uri ->
                            attachedImageUri.value = uri
                            showNoteCamera.value = false
                            noteCameraSessionKey.intValue += 1
                        },
                        onImageCleared = {
                            attachedImageUri.value = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            GarageButton(
                text = "SAVE NOTE",
                onClick = {
                    if (noteInputText.value.isNotBlank() || attachedImageUri.value != null) {
                        onNoteAdded(noteInputText.value, attachedImageUri.value)
                        noteInputText.value = ""
                        attachedImageUri.value = null
                    }
                },
                modifier = Modifier.padding(start = 40.dp),
                color = SafetyOrange,
                enabled = noteInputText.value.isNotBlank() || attachedImageUri.value != null
            )
        }
    }
}

