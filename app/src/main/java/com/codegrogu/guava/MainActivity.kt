package com.codegrogu.guava

import com.codegrogu.guava.ui.navigation.AppNavigation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.codegrogu.guava.service.AuthService
import com.codegrogu.guava.ui.theme.GuavaTheme
import com.codegrogu.guava.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    private lateinit var authService: AuthService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force IPv4 for emulator compatibility
        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv6Addresses", "false")
        
        authService = AuthService(appViewModel = appViewModel)
        authService.listenToAuthState(lifecycleScope)

        enableEdgeToEdge()
        setContent {
            GuavaTheme {
                AppNavigation(
                    appViewModel = appViewModel,
                    authService = authService
                )
            }
        }
    }
}
