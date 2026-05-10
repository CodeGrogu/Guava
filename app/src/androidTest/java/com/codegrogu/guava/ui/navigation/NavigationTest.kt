package com.codegrogu.guava.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.codegrogu.guava.model.User
import com.codegrogu.guava.model.UserRole
import com.codegrogu.guava.service.AuthService
import com.codegrogu.guava.viewmodel.AppViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var appViewModel: AppViewModel
    private lateinit var authService: AuthService

    @Before
    fun setup() {
        appViewModel = AppViewModel()
        authService = mock(AuthService::class.java)
    }

    @Test
    fun navigation_startsAtLogin() {
        composeTestRule.setContent {
            AppNavigation(appViewModel = appViewModel, authService = authService)
        }

        composeTestRule.onNodeWithText("VALENTINE'S GARAGE").assertIsDisplayed()
    }

    @Test
    fun navigation_navigatesToSignUpFromLogin() {
        composeTestRule.setContent {
            AppNavigation(appViewModel = appViewModel, authService = authService)
        }

        composeTestRule.onNodeWithText("INITIALIZE NEW ACCOUNT").performClick()

        composeTestRule.onNodeWithText("NEW REGISTRATION").assertIsDisplayed()
    }

    @Test
    fun navigation_navigatesToMechanicDashboardOnRoleMatch() {
        composeTestRule.setContent {
            AppNavigation(appViewModel = appViewModel, authService = authService)
        }

        appViewModel.updateUser(
            user = User("1", "Mechanic Bob", "bob@test.com"),
            role = UserRole.MECHANIC
        )

        composeTestRule.onNodeWithText("Mechanic Dashboard - Welcome Mechanic Bob").assertIsDisplayed()
    }

    @Test
    fun navigation_navigatesToManagerDashboardOnRoleMatch() {
        composeTestRule.setContent {
            AppNavigation(appViewModel = appViewModel, authService = authService)
        }

        appViewModel.updateUser(
            user = User("2", "Manager Alice", "alice@test.com"),
            role = UserRole.MANAGER
        )

        composeTestRule.onNodeWithText("Manager Dashboard - Welcome Manager Alice").assertIsDisplayed()
    }
}
