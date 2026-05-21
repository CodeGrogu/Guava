package com.codegrogu.guava.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.codegrogu.guava.model.User
import com.codegrogu.guava.model.UserRole
import com.codegrogu.guava.service.AuthService
import com.codegrogu.guava.viewmodel.AppViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var appViewModel: AppViewModel
    private lateinit var authService: AuthService

    @Before
    fun setup() {
        appViewModel = AppViewModel()
        authService = mock(AuthService::class.java)
        doAnswer {
            appViewModel.clearState()
            null
        }.`when`(authService).logoutUser()
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

        composeTestRule.onNodeWithText("MECHANIC DASHBOARD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome Mechanic Bob").assertIsDisplayed()
    }

    @Test
    fun navigation_navigatesToCheckInFromMechanicDashboard() {
        composeTestRule.setContent {
            AppNavigation(appViewModel = appViewModel, authService = authService)
        }

        appViewModel.updateUser(
            user = User("1", "Mechanic Bob", "bob@test.com"),
            role = UserRole.MECHANIC
        )

        composeTestRule.onNodeWithText("VEHICLE CHECK-IN").performClick()

        composeTestRule.onNodeWithText("Log new truck arrival").assertIsDisplayed()
        composeTestRule.onNodeWithText("TRUCK LICENSE PLATE / ID").assertIsDisplayed()
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

        composeTestRule.onNodeWithText("MANAGER DASHBOARD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome Manager Alice").assertIsDisplayed()
    }

    @Test
    fun navigation_logsOutFromMechanicDashboard() {
        composeTestRule.setContent {
            AppNavigation(appViewModel = appViewModel, authService = authService)
        }

        appViewModel.updateUser(
            user = User("1", "Mechanic Bob", "bob@test.com"),
            role = UserRole.MECHANIC
        )

        composeTestRule.onNodeWithText("LOG OUT").performClick()

        verify(authService).logoutUser()
        composeTestRule.onNodeWithText("VALENTINE'S GARAGE").assertIsDisplayed()
    }

    @Test
    fun navigation_logsOutFromManagerDashboard() {
        composeTestRule.setContent {
            AppNavigation(appViewModel = appViewModel, authService = authService)
        }

        appViewModel.updateUser(
            user = User("2", "Manager Alice", "alice@test.com"),
            role = UserRole.MANAGER
        )

        composeTestRule.onNodeWithText("LOG OUT").performClick()

        verify(authService).logoutUser()
        composeTestRule.onNodeWithText("VALENTINE'S GARAGE").assertIsDisplayed()
    }
}
