package com.codegrogu.guava.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.codegrogu.guava.model.UserRole
import com.codegrogu.guava.service.AuthService
import com.codegrogu.guava.ui.screens.LoginScreen
import com.codegrogu.guava.ui.screens.SignUpScreen
import com.codegrogu.guava.ui.screens.MechanicDashboardScreen
import com.codegrogu.guava.ui.screens.CheckInScreen
import com.codegrogu.guava.ui.screens.ManagerDashboardScreen
import com.codegrogu.guava.ui.screens.RepairWorkflowScreen
import com.codegrogu.guava.viewmodel.AppViewModel
import com.codegrogu.guava.viewmodel.UiEvent
import com.codegrogu.guava.ui.components.AppSnackbarVisuals
import com.codegrogu.guava.ui.theme.IndustrialBlack
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    appViewModel: AppViewModel,
    authService: AuthService
) {
    val navController = rememberNavController()
    val uiState by appViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun logoutToLogin(popUpDestination: Destination) {
        authService.logoutUser()
        navController.navigate(Destination.Login) {
            popUpTo(popUpDestination) { inclusive = true }
            launchSingleTop = true
        }
    }

    LaunchedEffect(Unit) {
        appViewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        AppSnackbarVisuals(
                            message = event.message,
                            isError = event.isError
                        )
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                val visuals = data.visuals as? AppSnackbarVisuals
                val container = when {
                    visuals == null -> MaterialTheme.colorScheme.surface
                    visuals.isError -> MaterialTheme.colorScheme.errorContainer
                    else -> IndustrialBlack
                }
                val content = when {
                    visuals == null -> MaterialTheme.colorScheme.onSurface
                    visuals.isError -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onPrimary
                }

                Snackbar(
                    snackbarData = data,
                    containerColor = container,
                    contentColor = content
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Login,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Destination.Login> {
                LoginScreen(
                    onLoginSuccess = { email, password ->
                        scope.launch {
                            appViewModel.setLoading(true)
                            try {
                                val result = authService.loginUser(email, password)
                                if (result.isFailure) {
                                    appViewModel.showSnackbar(result.exceptionOrNull()?.message ?: "Login Failed")
                                }
                            } finally {
                                appViewModel.setLoading(false)
                            }
                        }
                    },
                    isLoading = uiState.isLoading,
                    onNavigateToSignUp = {
                        navController.navigate(Destination.SignUp) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<Destination.SignUp> {
                SignUpScreen(
                    onSignUp = { email, password, name, role ->
                        scope.launch {
                            appViewModel.setLoading(true)
                            try {
                                val result = authService.registerUser(email, password, name, role)
                                if (result.isFailure) {
                                    appViewModel.showSnackbar(result.exceptionOrNull()?.message ?: "Registration Failed")
                                } else {
                                    appViewModel.showSnackbar("Account created successfully!", isError = false)
                                }
                            } finally {
                                appViewModel.setLoading(false)
                            }
                        }
                    },
                    isLoading = uiState.isLoading,
                    onNavigateToLogin = {
                        navController.navigate(Destination.Login) {
                            popUpTo(Destination.Login) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<Destination.MechanicDashboard> {
                MechanicDashboardScreen(
                    userName = uiState.currentUser?.name ?: "Mechanic",
                    onNavigateToCheckIn = {
                        navController.navigate(Destination.CheckIn) {
                            launchSingleTop = true
                        }
                    },
                    onOpenVehicle = { vehicle ->
                        navController.navigate(
                            Destination.RepairWorkflow(
                                vehicleId = vehicle.vehicleId,
                                licensePlate = vehicle.licensePlate
                            )
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onLogout = {
                        logoutToLogin(Destination.MechanicDashboard)
                    }
                )
            }

            composable<Destination.CheckIn> {
                CheckInScreen(
                    viewModel = appViewModel,
                    onCheckInComplete = {
                        navController.navigate(Destination.MechanicDashboard) {
                            popUpTo(Destination.CheckIn) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Destination.RepairWorkflow> { backStackEntry ->
                val destination = backStackEntry.toRoute<Destination.RepairWorkflow>()
                val currentUser = uiState.currentUser

                if (currentUser == null) {
                    Text("Session expired. Please log in again.")
                } else {
                    RepairWorkflowScreen(
                        vehicleId = destination.vehicleId,
                        vehicleLabel = destination.licensePlate,
                        currentUser = currentUser,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            composable<Destination.ManagerDashboard> {
                ManagerDashboardScreen(
                    appViewModel = appViewModel,
                    onLogout = {
                        logoutToLogin(Destination.ManagerDashboard)
                    }
                )
            }
        }
    }

    // Role-based navigation reaction
    LaunchedEffect(uiState.currentUser?.id, uiState.userRole) {
        if (uiState.currentUser != null) {
            when (uiState.userRole) {
                UserRole.MECHANIC -> {
                    navController.navigate(Destination.MechanicDashboard) {
                        popUpTo(Destination.Login) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                UserRole.MANAGER -> {
                    navController.navigate(Destination.ManagerDashboard) {
                        popUpTo(Destination.Login) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                else -> {}
            }
        }
    }
}
