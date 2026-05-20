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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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

// ─────────────────────────────────────────────────────────────────────────────
// TASK LIST ITEM COMPONENT
//
// This is a single row that displays one repair task.
// It shows:
//   - A checkbox: The mechanic taps it to mark the task complete/incomplete
//   - The task description: What needs to be fixed (e.g., "Replace clutch")
//   - (Conditionally) "Completed by [Name]": Shows which mechanic finished it
//
// This component is reusable and will be placed in a scrollable list in
// RepairWorkflowScreen so the mechanic can see ALL tasks for a vehicle.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TaskListItem(
    // The repair task data to display
    task: RepairTask,

    // The current mechanic's Firebase auth UID
    // (needed to know if THIS mechanic completed the task)
    currentMechanicUid: String,

    // The current mechanic's name for display
    // (needed if they toggle the task in this session)
    currentMechanicName: String,

    // Callback: Called when the mechanic taps the checkbox
    // Passes: whether to mark complete (true) or incomplete (false)
    onTaskToggle: (isComplete: Boolean) -> Unit,

    // Callback: Called when the mechanic saves a new note
    // Passes: the note text and optional attached image URI
    onNoteAdded: (noteText: String, imageUri: Uri?) -> Unit,

    // Optional modifier for styling/layout
    modifier: Modifier = Modifier
) {
    // Local state: Is the notes section expanded or collapsed?
    // Starts collapsed by default (remember { mutableStateOf(false) })
    val isNotesExpanded = remember { mutableStateOf(false) }

    // Local state: The text the mechanic is typing in the note input field
    val noteInputText = remember { mutableStateOf("") }
    val attachedImageUri = remember { mutableStateOf<Uri?>(null) }
    val showNoteCamera = remember { mutableStateOf(false) }
    val noteCameraSessionKey = remember { mutableStateOf(0) }

    // The entire component is a Column so we can stack multiple sections
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // ─────────────────────────────────────────────────────────
        // FIRST ROW: Checkbox + Task Description + Expand Icon
        // ─────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // When user taps anywhere on this row (except checkbox), toggle expand
                    isNotesExpanded.value = !isNotesExpanded.value
                }
        ) {
            // The Material Design checkbox
            // When the mechanic taps it, onTaskToggle is called with the new state
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { newState ->
                    onTaskToggle(newState)
                },
                // Use SafetyOrange as the accent color to match the industrial theme
                colors = CheckboxDefaults.colors(
                    checkedColor = SafetyOrange,
                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    checkmarkColor = MaterialTheme.colorScheme.background
                )
            )

            // Add some space between the checkbox and the text
            Spacer(modifier = Modifier.width(12.dp))

            // The task description (what needs to be fixed)
            // Uses monospace font to match the garage industrial theme
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

        // ─────────────────────────────────────────────────────────
        // SECOND ROW: "Completed by [Name]" (only if task is done)
        // ─────────────────────────────────────────────────────────
        if (task.isCompleted && task.completedByName.isNotEmpty()) {
            // Add a little spacing between the checkbox row and this text
            Spacer(modifier = Modifier.height(8.dp))

            // Show which mechanic completed this task
            // This text is indented slightly to align under the task description
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

        // ─────────────────────────────────────────────────────────
        // NOTES SECTION (COLLAPSIBLE)
        // Only shows if user has expanded it OR if there are existing notes
        // ─────────────────────────────────────────────────────────
        if (isNotesExpanded.value) {
            Spacer(modifier = Modifier.height(12.dp))

            // EXISTING NOTES: Display all notes that mechanics have already written
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

                // Display each note with the mechanic's name and the note text
                for (note in task.notes) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp)
                    ) {
                        // Show the mechanic's name in a smaller, dimmed text
                        Text(
                            text = "— ${note.mechanicName}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )

                        // Show the actual note text
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

            // INPUT FIELD: Allow the mechanic to type a new note
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
                key(noteCameraSessionKey.value) {
                    CameraCapture(
                        onImageCaptured = { uri ->
                            attachedImageUri.value = uri
                            showNoteCamera.value = false
                            noteCameraSessionKey.value += 1
                        },
                        onImageCleared = {
                            attachedImageUri.value = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SAVE NOTE BUTTON: When tapped, send the note text to the parent
            // Then clear the input field so it's ready for the next note
            GarageButton(
                text = "SAVE NOTE",
                onClick = {
                    if (noteInputText.value.isNotBlank() || attachedImageUri.value != null) {
                        onNoteAdded(noteInputText.value, attachedImageUri.value)
                        noteInputText.value = ""  // Clear the input
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

