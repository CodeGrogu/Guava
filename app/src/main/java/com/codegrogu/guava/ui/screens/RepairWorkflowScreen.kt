package com.codegrogu.guava.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.codegrogu.guava.model.User
import com.codegrogu.guava.service.RepairService
import com.codegrogu.guava.ui.components.TaskListItem
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// REPAIR WORKFLOW SCREEN
//
// This is the main screen where a mechanic views and works on all repair tasks
// for a specific vehicle.
//
// Flow:
// 1. Screen loads, LaunchedEffect triggers subscribeToVehicleTasks()
// 2. RepairService.subscribeToVehicleTasks() opens a real-time listener to Firestore
// 3. Initial task list is shown immediately
// 4. If another mechanic updates a task, the Flow emits new data and screen re-renders
// 5. All mechanics viewing this vehicle see changes in real-time
//
// This is WHERE ALL THE REAL-TIME MAGIC HAPPENS!
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RepairWorkflowScreen(
    // The vehicle ID to fetch tasks for
    vehicleId: String,

    // The current mechanic's information (for attribution when they update tasks)
    currentUser: User,

    // Optional modifier for layout
    modifier: Modifier = Modifier
) {
    // Snackbar state: Used to show error/success messages to the mechanic
    // (e.g., "Note saved!", "Error updating task")
    val snackbarHostState = remember { SnackbarHostState() }

    // Coroutine scope for launching async operations (Firestore updates)
    // rememberCoroutineScope gives us a scope tied to this composable's lifecycle
    val coroutineScope = rememberCoroutineScope()

    // Subscribe to real-time task updates from Firestore
    // - Initial collection happens here
    // - subscribeToVehicleTasks() returns a Flow<List<RepairTask>>
    // - collectAsState() converts the Flow to a Compose State
    //   (so when Flow emits new data, the screen re-renders automatically)
    val tasksFlow = remember { RepairService.subscribeToVehicleTasks(vehicleId) }
    val tasksState = tasksFlow.collectAsState(initial = null)
    val tasks = tasksState.value

    // When the composable first enters the screen, log that we're loading tasks
    LaunchedEffect(vehicleId) {
        // The listener is set up when tasksFlow is created, so we don't need
        // to do anything extra here. But this hook ensures we refresh if
        // vehicleId changes (in case the mechanic navigates between vehicles)
    }

    // The main layout: Scaffold provides standard app structure
    // (app bar, snackbar, content area)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // The topBar could have the vehicle ID, but keeping it simple for now
    ) { paddingValues ->
        // MAIN CONTENT AREA
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // HEADER: Show the vehicle ID and task count
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "VEHICLE: $vehicleId",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    // Show task count (e.g., "3 tasks to complete")
                    val taskCount = tasks?.size ?: 0
                    Text(
                        text = "$taskCount ${if (taskCount == 1) "task" else "tasks"}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // CONTENT: Show tasks or loading state
            when {
                // LOADING STATE: Show spinner if tasks haven't loaded yet
                tasks == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Loading tasks...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }

                // EMPTY STATE: No tasks for this vehicle
                tasks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No repair tasks yet",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }

                // TASK LIST: Display all tasks in a scrollable list
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                    ) {
                        items(
                            items = tasks,
                            // Unique key for each item (so Compose can track changes efficiently)
                            key = { task -> task.id }
                        ) { task ->
                            // For each task, display a TaskListItem component
                            TaskListItem(
                                task = task,
                                currentMechanicUid = currentUser.id,
                                currentMechanicName = currentUser.name,

                                // When the mechanic toggles the checkbox
                                onTaskToggle = { isComplete ->
                                    // Call the service to update Firestore in a background coroutine
                                    coroutineScope.launch {
                                        val result = RepairService.toggleTaskCompletion(
                                            vehicleId = vehicleId,
                                            taskId = task.id,
                                            isComplete = isComplete,
                                            mechanicUid = currentUser.id,
                                            mechanicName = currentUser.name
                                        )

                                        // Check if the result was successful or failed
                                        result.onSuccess {
                                            // Success! Task was updated in Firestore
                                            // The Flow will automatically emit the new data
                                            // and the screen will re-render
                                            if (isComplete) {
                                                snackbarHostState.showSnackbar(
                                                    message = "Task completed!",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }

                                        result.onFailure { error ->
                                            // Something went wrong (network, permissions, etc.)
                                            snackbarHostState.showSnackbar(
                                                message = "Error updating task: ${error.message}",
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    }
                                },

                                // When the mechanic saves a new note
                                onNoteAdded = { noteText ->
                                    // Call the service to add the note to Firestore in a background coroutine
                                    coroutineScope.launch {
                                        val result = RepairService.addNoteToTask(
                                            vehicleId = vehicleId,
                                            taskId = task.id,
                                            noteText = noteText,
                                            mechanicUid = currentUser.id,
                                            mechanicName = currentUser.name
                                        )

                                        result.onSuccess {
                                            // Success! Note was added to Firestore
                                            // The Flow will automatically emit the new data
                                            // and the screen will re-render with the new note
                                            snackbarHostState.showSnackbar(
                                                message = "Note saved!",
                                                duration = SnackbarDuration.Short
                                            )
                                        }

                                        result.onFailure { error ->
                                            // Something went wrong
                                            snackbarHostState.showSnackbar(
                                                message = "Error saving note: ${error.message}",
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

