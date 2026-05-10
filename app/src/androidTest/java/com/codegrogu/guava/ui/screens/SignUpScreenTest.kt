package com.codegrogu.guava.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.codegrogu.guava.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SignUpScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun signUpScreen_showsAllComponents() {
        composeTestRule.setContent {
            SignUpScreen(onSignUp = { _, _, _, _ -> })
        }

        composeTestRule.onNodeWithText("NEW REGISTRATION").assertIsDisplayed()
        composeTestRule.onNodeWithText("FULL IDENTIFIER (NAME)").assertIsDisplayed()
        composeTestRule.onNodeWithText("COMMS ADDRESS (EMAIL)").assertIsDisplayed()
        composeTestRule.onNodeWithText("ACCESS CODE").assertIsDisplayed()
        composeTestRule.onNodeWithText("MECHANIC").assertIsDisplayed()
        composeTestRule.onNodeWithText("MANAGER").assertIsDisplayed()
        composeTestRule.onNodeWithTag("SignUpSubmitButton").assertIsNotEnabled()
    }

    @Test
    fun signUpScreen_showsValidationMessagesForInvalidInput() {
        composeTestRule.setContent {
            SignUpScreen(onSignUp = { _, _, _, _ -> })
        }

        composeTestRule.onNodeWithTag("SignUpNameField").performTextInput("SingleName")
        composeTestRule.onNodeWithTag("SignUpEmailField").performTextInput("bad-email")
        composeTestRule.onNodeWithTag("SignUpPasswordField").performTextInput("short")

        composeTestRule.onNodeWithText("Enter a first and last name.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter a valid email address.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password must be 8+ chars with upper, lower, and digit.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("SignUpSubmitButton").assertIsNotEnabled()
    }

    @Test
    fun signUpScreen_requiresExplicitRoleSelection() {
        composeTestRule.setContent {
            SignUpScreen(onSignUp = { _, _, _, _ -> })
        }

        composeTestRule.onNodeWithTag("SignUpNameField").performTextInput("Alex Morgan")
        composeTestRule.onNodeWithTag("SignUpEmailField").performTextInput("alex@example.com")
        composeTestRule.onNodeWithTag("SignUpPasswordField").performTextInput("Password1")

        composeTestRule.onNodeWithText("Select Mechanic or Manager.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("SignUpSubmitButton").assertIsNotEnabled()
    }

    @Test
    fun signUpScreen_submitsValidRegistration() {
        var submittedEmail: String? = null
        var submittedPassword: String? = null
        var submittedName: String? = null
        var submittedRole: UserRole? = null

        composeTestRule.setContent {
            SignUpScreen(
                onSignUp = { email, password, name, role ->
                    submittedEmail = email
                    submittedPassword = password
                    submittedName = name
                    submittedRole = role
                }
            )
        }

        composeTestRule.onNodeWithTag("SignUpNameField").performTextInput("Jamie Manager")
        composeTestRule.onNodeWithTag("SignUpEmailField").performTextInput("jamie@example.com")
        composeTestRule.onNodeWithTag("SignUpPasswordField").performTextInput("Password1")
        composeTestRule.onNodeWithTag("ManagerRoleCard").performClick()
        composeTestRule.onNodeWithTag("SignUpSubmitButton").assertIsEnabled()
        composeTestRule.onNodeWithTag("SignUpSubmitButton").performClick()

        composeTestRule.runOnIdle {
            assertEquals("jamie@example.com", submittedEmail)
            assertEquals("Password1", submittedPassword)
            assertEquals("Jamie Manager", submittedName)
            assertEquals(UserRole.MANAGER, submittedRole)
        }
    }

    @Test
    fun signUpScreen_showsLoading() {
        composeTestRule.setContent {
            SignUpScreen(onSignUp = { _, _, _, _ -> }, isLoading = true)
        }

        composeTestRule.onNodeWithTag("SignUpLoadingIndicator").assertIsDisplayed()
    }
}
