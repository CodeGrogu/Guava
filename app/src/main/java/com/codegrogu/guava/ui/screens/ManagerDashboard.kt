package com.codegrogu.guava.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codegrogu.guava.firebase.FirebaseConfig
import com.codegrogu.guava.model.DateRange
import com.codegrogu.guava.model.User
import com.codegrogu.guava.model.Vehicle
import com.codegrogu.guava.service.EmployeePerformanceReport
import com.codegrogu.guava.service.ReportService
import com.codegrogu.guava.ui.components.*
import com.codegrogu.guava.ui.theme.IndustrialBlack
import com.codegrogu.guava.ui.theme.SafetyOrange
import com.codegrogu.guava.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(
    appViewModel: AppViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("EMPLOYEES", "VEHICLE LOGS")
    
    var selectedFilter by remember { mutableStateOf("Today") }
    val filters = listOf("Today", "This Week", "This Month")

    val scope = rememberCoroutineScope()

    var employeeReports by remember { mutableStateOf<List<EmployeePerformanceReport>>(emptyList()) }
    var vehicleLogs by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var isLoadingReports by remember { mutableStateOf(false) }

    fun fetchReports() {
        scope.launch {
            isLoadingReports = true
            val calendar = Calendar.getInstance()
            val end = calendar.time
            
            when (selectedFilter) {
                "Today" -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                }
                "This Week" -> {
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                }
                "This Month" -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                }
            }
            val start = calendar.time
            val dateRange = DateRange(start, end)

            if (selectedTab == 0) {
                // Fetch mechanics first
                try {
                    val usersSnapshot = FirebaseConfig.firestore.collection("users")
                        .whereEqualTo("role", "MECHANIC")
                        .get().await()
                    
                    val mechanics = usersSnapshot.documents.map { doc ->
                        User(doc.id, doc.getString("name") ?: "Unknown", doc.getString("email") ?: "")
                    }

                    val reports = mechanics.map { mechanic ->
                        ReportService.getEmployeePerformanceReport(
                            mechanicId = mechanic.id,
                            mechanicName = mechanic.name,
                            dateRange = dateRange
                        ).getOrDefault(EmployeePerformanceReport(mechanic.id, mechanic.name, 0, emptyList()))
                    }
                    employeeReports = reports
                } catch (e: Exception) {
                    appViewModel.showSnackbar("Failed to fetch mechanics: ${e.message}")
                }
            } else {
                val result = ReportService.getVehicleIntakeReport(dateRange)
                vehicleLogs = result.getOrDefault(emptyList())
            }
            isLoadingReports = false
        }
    }

    LaunchedEffect(selectedTab, selectedFilter) {
        fetchReports()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GarageBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "MANAGER OVERSIGHT",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Valentine's Garage Admin v1.0",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = SafetyOrange
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // TabRow
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = SafetyOrange,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (isSelected) SafetyOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    filter.uppercase(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = if (selectedFilter == filter) IndustrialBlack
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SafetyOrange,
                                selectedLabelColor = IndustrialBlack,
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (selectedFilter == filter) SafetyOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                enabled = true,
                                selected = selectedFilter == filter
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Polish: Export Button
                    IconButton(
                        onClick = { appViewModel.showSnackbar("Exporting report...", isError = false) },
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.dp, SafetyOrange, MaterialTheme.shapes.extraSmall)
                    ) {
                        Text("PDF", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SafetyOrange)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoadingReports) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SafetyOrange)
                    }
                } else if (selectedTab == 0 && employeeReports.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No employee activity for this range.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else if (selectedTab == 1 && vehicleLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No vehicle logs for this range.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (selectedTab == 0) {
                            items(employeeReports) { report ->
                                EmployeeReportCard(report = report)
                            }
                        } else {
                            items(vehicleLogs) { vehicle ->
                                VehicleReportCard(vehicle = vehicle)
                            }
                        }
                    }
                }
            }
        }
    }
}
