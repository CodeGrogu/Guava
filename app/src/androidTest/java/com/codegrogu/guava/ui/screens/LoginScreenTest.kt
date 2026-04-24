package com.codegrogu.guava.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_showsAllComponents() {
        composeTestRule.setContent {
            LoginScreen(onLoginSuccess = { _, _ -> })
        }

        composeTestRule.onNodeWithText("Guava Login").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
    }

    @Test
    fun loginScreen_buttonDisabledWhenEmpty() {
        composeTestRule.setContent {
            LoginScreen(onLoginSuccess = { _, _ -> })
        }

        composeTestRule.onNodeWithText("Login").assertIsNotEnabled()
    }

    @Test
    fun loginScreen_showsErrorMessage() {
        val error = "Invalid credentials"
        composeTestRule.setContent {
            LoginScreen(onLoginSuccess = { _, _ -> }, errorMessage = error)
        }

        composeTestRule.onNodeWithText(error).assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsLoading() {
        composeTestRule.setContent {
            LoginScreen(onLoginSuccess = { _, _ -> }, isLoading = true)
        }

        composeTestRule.onNodeWithTag("LoginLoadingIndicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login").assertDoesNotExist()
    }
}
