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

        composeTestRule.onNodeWithText("VALENTINE'S GARAGE").assertIsDisplayed()
        composeTestRule.onNodeWithText("TERMINAL ID (EMAIL)").assertIsDisplayed()
        composeTestRule.onNodeWithText("PASSCODE").assertIsDisplayed()
        composeTestRule.onNodeWithText("AUTHORIZE").assertIsDisplayed()
        composeTestRule.onNodeWithText("INITIALIZE NEW ACCOUNT").assertIsDisplayed()
    }

    @Test
    fun loginScreen_buttonDisabledWhenEmpty() {
        composeTestRule.setContent {
            LoginScreen(onLoginSuccess = { _, _ -> })
        }

        composeTestRule.onNodeWithTag("LoginSubmitButton").assertIsNotEnabled()
    }

    @Test
    fun loginScreen_showsLoading() {
        composeTestRule.setContent {
            LoginScreen(onLoginSuccess = { _, _ -> }, isLoading = true)
        }

        composeTestRule.onNodeWithTag("LoginLoadingIndicator").assertIsDisplayed()
    }
}
