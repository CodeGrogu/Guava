package com.codegrogu.guava.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.OutlinedTextField
import com.codegrogu.guava.model.Vehicle
import com.codegrogu.guava.service.VehicleService
import com.codegrogu.guava.ui.components.GarageButton
import com.codegrogu.guava.ui.components.GarageImagePreview
import com.codegrogu.guava.ui.theme.SafetyOrange
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairWorkflowScreen(
    // The vehicle ID to fetch tasks for
    vehicleId: String,

    vehicleLabel: String = vehicleId,

    // The current mechanic's information (for attribution when they update tasks)
    currentUser: User,

    onNavigateBack: () -> Unit = {},

    // Optional modifier for layout
    modifier: Modifier = Modifier
) {
    // Snackbar state: Used to show error/success messages to the mechanic
    // (e.g., "Note saved!", "Error updating task")
    val snackbarHostState = remember { SnackbarHostState() }

    // Coroutine scope for launching async operations (Firestore updates)
    // rememberCoroutineScope gives us a scope tied to this composable's lifecycle
    val coroutineScope = rememberCoroutineScope()
    var newTaskText by remember { mutableStateOf("") }
    var isAddingTask by remember { mutableStateOf(false) }
    var vehicle by remember { mutableStateOf<Vehicle?>(null) }
    var vehicleLoadError by remember { mutableStateOf<String?>(null) }

    // Subscribe to real-time task updates from Firestore
    // - Initial collection happens here
    // - subscribeToVehicleTasks() returns a Flow<List<RepairTask>>
    // - collectAsState() converts the Flow to a Compose State
    //   (so when Flow emits new data, the screen re-renders automatically)
    val tasksFlow = remember(vehicleId) { RepairService.subscribeToVehicleTasks(vehicleId) }
    val tasksState = tasksFlow.collectAsState(initial = null)
    val tasks = tasksState.value

    // When the composable first enters the screen, log that we're loading tasks
    LaunchedEffect(vehicleId) {
        VehicleService.getVehicle(vehicleId)
            .onSuccess {
                vehicle = it
                vehicleLoadError = null
            }
            .onFailure {
                vehicleLoadError = it.message ?: "Unable to load vehicle details."
            }
    }

    // The main layout: Scaffold provides standard app structure
    // (app bar, snackbar, content area)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "REPAIR WORKFLOW",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SafetyOrange
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text = "VEHICLE: $vehicleLabel",
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

                    val vehicleImages = vehicle?.conditionImageUrls.orEmpty()
                    if (vehicleImages.isNotEmpty()) {
                        Text(
                            text = "CHECK-IN PHOTOS",
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                            )
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = vehicleImages,
                                key = { imageUrl -> imageUrl }
                            ) { imageUrl ->
                                GarageImagePreview(
                                    imageUrl = imageUrl,
                                    contentDescription = "Vehicle check-in photo",
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(88.dp)
                                )
                            }
                        }
                    } else if (vehicleLoadError != null) {
                        Text(
                            text = vehicleLoadError ?: "",
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    modifier = Modifier.weight(1f),
                    label = {
                        Text(
                            text = "Add repair task",
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    singleLine = true,
                    enabled = !isAddingTask
                )

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))

                GarageButton(
                    text = "ADD",
                    enabled = newTaskText.isNotBlank() && !isAddingTask,
                    isLoading = isAddingTask,
                    modifier = Modifier.width(96.dp),
                    onClick = {
                        val taskDescription = newTaskText
                        coroutineScope.launch {
                            isAddingTask = true

                            RepairService.addRepairTask(
                                vehicleId = vehicleId,
                                description = taskDescription,
                                mechanicUid = currentUser.id,
                                mechanicName = currentUser.name
                            )
                                .onSuccess {
                                    newTaskText = ""
                                    snackbarHostState.showSnackbar(
                                        message = "Task added.",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                .onFailure { error ->
                                    snackbarHostState.showSnackbar(
                                        message = "Error adding task: ${error.message}",
                                        duration = SnackbarDuration.Long
                                    )
                                }

                            isAddingTask = false
                        }
                    }
                )
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
                            text = "No repair tasks yet. Add the first repair task above.",
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

