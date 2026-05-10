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
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                val visuals = data.visuals as? UiEvent.ShowSnackbar
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
                            val result = authService.loginUser(email, password)
                            if (result.isFailure) {
                                appViewModel.showSnackbar(result.exceptionOrNull()?.message ?: "Login Failed")
                            }
                            appViewModel.setLoading(false)
                        }
                    },
                    isLoading = uiState.isLoading,
                    onNavigateToSignUp = { navController.navigate(Destination.SignUp) }
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
                                navController.navigate(Destination.Login) {
                                    popUpTo(Destination.SignUp) { inclusive = true }
                                }
                            }
                            appViewModel.setLoading(false)
                        }
                    },
                    isLoading = uiState.isLoading,
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable<Destination.MechanicDashboard> {
                // Example of a Dashboard that can navigate to Check-In
                MechanicDashboardScreen(
                    userName = uiState.currentUser?.name ?: "Mechanic",
                    onNavigateToCheckIn = { navController.navigate(Destination.CheckIn) }
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

    // Auth Watcher: Moves user to dashboard upon login
    LaunchedEffect(uiState.currentUser, uiState.userRole) {
        val user = uiState.currentUser
        if (user != null) {
            val target = when (uiState.userRole) {
                UserRole.MECHANIC -> Destination.MechanicDashboard
                UserRole.MANAGER -> Destination.ManagerDashboard
                else -> null
            }
            target?.let {
                navController.navigate(it) {
                    popUpTo(Destination.Login) { inclusive = true }
                }
            }
        }
    }
}