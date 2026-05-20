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
import com.codegrogu.guava.ui.screens.CheckInScreen
import com.codegrogu.guava.ui.screens.LoginScreen
import com.codegrogu.guava.ui.screens.MechanicDashboardScreen
import com.codegrogu.guava.ui.screens.ManagerDashboardScreen
import com.codegrogu.guava.ui.screens.SignUpScreen
import com.codegrogu.guava.viewmodel.AppViewModel
import com.codegrogu.guava.viewmodel.UiEvent
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
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                // Basic Snackbar implementation; can be customized further if AppSnackbarVisuals is defined
                Snackbar(
                    snackbarData = data
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
                        navController.navigate(Destination.SignUp)
                    }
                )
            }

            composable<Destination.SignUp> {
                SignUpScreen(
                    onSignUp = { email, password, name, role ->
                        scope.launch {
                            appViewModel.setLoading(true)
                            val result = authService.registerUser(email, password, name, role)
                            if (result.isFailure) {
                                appViewModel.showSnackbar(result.exceptionOrNull()?.message ?: "Registration Failed")
                            } else {
                                appViewModel.showSnackbar("Account created successfully!", isError = false)
                            }
                            appViewModel.setLoading(false)
                        }
                    },
                    isLoading = uiState.isLoading,
                    onNavigateToLogin = {
                        navController.navigate(Destination.Login) {
                            popUpTo(Destination.Login) { inclusive = true }
                        }
                    }
                )
            }

            composable<Destination.MechanicDashboard> {
                MechanicDashboardScreen(
                    userName = uiState.currentUser?.name ?: "Mechanic",
                    onNavigateToCheckIn = { navController.navigate(Destination.CheckIn) }
                )
            }

            composable<Destination.CheckIn> {
                CheckInScreen(
                    viewModel = appViewModel,
                    onCheckInComplete = {
                        navController.navigate(Destination.MechanicDashboard) {
                            popUpTo(Destination.CheckIn) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Destination.ManagerDashboard> {
                ManagerDashboardScreen(
                    appViewModel = appViewModel,
                    onLogout = {
                        authService.logoutUser()
                        navController.navigate(Destination.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(uiState.currentUser, uiState.userRole) {
        if (uiState.currentUser != null) {
            when (uiState.userRole) {
                UserRole.MECHANIC -> {
                    navController.navigate(Destination.MechanicDashboard) {
                        popUpTo(Destination.Login) { inclusive = true }
                    }
                }
                UserRole.MANAGER -> {
                    navController.navigate(Destination.ManagerDashboard) {
                        popUpTo(Destination.Login) { inclusive = true }
                    }
                }
                else -> {}
            }
        }
    }
}