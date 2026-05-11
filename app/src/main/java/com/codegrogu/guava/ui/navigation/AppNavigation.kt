package com.codegrogu.guava.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.codegrogu.guava.model.UserRole
import com.codegrogu.guava.service.AuthService
import com.codegrogu.guava.ui.screens.LoginScreen
import com.codegrogu.guava.ui.screens.SignUpScreen
import com.codegrogu.guava.ui.screens.MechanicDashboardScreen
import com.codegrogu.guava.ui.screens.CheckInScreen
import com.codegrogu.guava.viewmodel.AppViewModel
import com.codegrogu.guava.viewmodel.UiEvent
import com.codegrogu.guava.ui.components.AppSnackbarVisuals
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
                val isError = visuals?.isError ?: true
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer
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
                // Example of a Dashboard that can navigate to Check-In
                MechanicDashboardScreen(
                    userName = uiState.currentUser?.name ?: "Mechanic",
                    onNavigateToCheckIn = {
                        navController.navigate(Destination.CheckIn) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // --- THE NEW CHECK-IN ROUTE ---
            composable<Destination.CheckIn> {
                CheckInScreen(
                    viewModel = appViewModel,
                    onCheckInComplete = {
                        // Return to Dashboard and clear the Check-In screen from backstack
                        navController.navigate(Destination.MechanicDashboard) {
                            popUpTo(Destination.CheckIn) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Destination.ManagerDashboard> {
                Text("Manager Dashboard - Welcome ${uiState.currentUser?.name}")
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
