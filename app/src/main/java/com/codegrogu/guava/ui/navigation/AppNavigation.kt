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

    // Listen for global UI events (Error/Success messages)
    LaunchedEffect(Unit) {
        appViewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.toSnackbarVisuals())
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
                            popUpTo(Destination.Login) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<Destination.MechanicDashboard> {
                Surface(modifier = Modifier.padding(innerPadding)) {
                    Text("Mechanic Dashboard - Welcome ${uiState.currentUser?.name}")
                }
            }

            composable<Destination.ManagerDashboard> {
                Surface(modifier = Modifier.padding(innerPadding)) {
                    Text("Manager Dashboard - Welcome ${uiState.currentUser?.name}")
                }
            }
        }
    }

    // Role-based navigation reaction
    LaunchedEffect(uiState.currentUser?.id, uiState.userRole) {
        when {
            uiState.currentUser == null -> {
                navController.navigate(Destination.Login) {
                    launchSingleTop = true
                }
            }
            uiState.userRole == UserRole.MECHANIC -> {
                navController.navigate(Destination.MechanicDashboard) {
                    popUpTo(Destination.Login) { inclusive = true }
                    launchSingleTop = true
                }
            }
            uiState.userRole == UserRole.MANAGER -> {
                navController.navigate(Destination.ManagerDashboard) {
                    popUpTo(Destination.Login) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
}

private data class AppSnackbarVisuals(
    override val message: String,
    val isError: Boolean,
    override val actionLabel: String? = null,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    override val withDismissAction: Boolean = true
) : SnackbarVisuals

private fun UiEvent.ShowSnackbar.toSnackbarVisuals(): SnackbarVisuals {
    return AppSnackbarVisuals(
        message = message,
        isError = isError
    )
}
