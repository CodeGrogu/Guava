package com.codegrogu.guava.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codegrogu.guava.model.UserRole
import com.codegrogu.guava.ui.components.GarageBackground
import com.codegrogu.guava.ui.components.GarageButton
import com.codegrogu.guava.ui.components.GarageTextField
import com.codegrogu.guava.ui.components.RoleCard
import com.codegrogu.guava.util.ValidationUtils

@Composable
fun SignUpScreen(
    onSignUp: (String, String, String, UserRole) -> Unit,
    isLoading: Boolean = false,
    onNavigateToLogin: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf<UserRole?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    val trimmedName = name.trim()
    val trimmedEmail = email.trim()
    val showNameError = name.isNotBlank() && !ValidationUtils.isValidName(trimmedName)
    val showEmailError = email.isNotBlank() && !ValidationUtils.isValidEmail(trimmedEmail)
    val showPasswordError = password.isNotBlank() && !ValidationUtils.isValidPassword(password)
    val showRoleError = role == null && name.isNotBlank() && email.isNotBlank() && password.isNotBlank()
    val isFormValid = ValidationUtils.isValidName(trimmedName) &&
        ValidationUtils.isValidEmail(trimmedEmail) &&
        ValidationUtils.isValidPassword(password) &&
        role != null

    Box(modifier = Modifier.fillMaxSize()) {
        GarageBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "NEW REGISTRATION",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Text(
                text = "ESTABLISH SYSTEM CREDENTIALS",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            GarageTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Identifier (Name)",
                isError = showNameError,
                fieldTag = "SignUpNameField",
                leadingIcon = Icons.Default.Person
            )
            ValidationMessage(
                message = "Enter a first and last name.",
                visible = showNameError
            )

            Spacer(modifier = Modifier.height(16.dp))

            GarageTextField(
                value = email,
                onValueChange = { email = it },
                label = "Comms Address (Email)",
                leadingIcon = Icons.Default.Email,
                isError = showEmailError,
                fieldTag = "SignUpEmailField",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            ValidationMessage(
                message = "Enter a valid email address.",
                visible = showEmailError
            )

            Spacer(modifier = Modifier.height(16.dp))

            GarageTextField(
                value = password,
                onValueChange = { password = it },
                label = "Access Code",
                leadingIcon = Icons.Default.Lock,
                isError = showPasswordError,
                fieldTag = "SignUpPasswordField",
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                "Hide password"
                            } else {
                                "Show password"
                            }
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            ValidationMessage(
                message = "Password must be 8+ chars with upper, lower, and digit.",
                visible = showPasswordError
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "SELECT DESIGNATION:",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                RoleCard(
                    title = "Mechanic",
                    icon = Icons.Default.Build,
                    selected = role == UserRole.MECHANIC,
                    onClick = { role = UserRole.MECHANIC },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("MechanicRoleCard")
                )
                Spacer(modifier = Modifier.width(16.dp))
                RoleCard(
                    title = "Manager",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    selected = role == UserRole.MANAGER,
                    onClick = { role = UserRole.MANAGER },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ManagerRoleCard")
                )
            }
            ValidationMessage(
                message = "Select Mechanic or Manager.",
                visible = showRoleError
            )

            Spacer(modifier = Modifier.height(48.dp))

            GarageButton(
                text = "Initialize",
                onClick = {
                    role?.let { selectedRole ->
                        if (isFormValid) {
                            onSignUp(trimmedEmail, password, trimmedName, selectedRole)
                        }
                    }
                },
                modifier = Modifier.testTag("SignUpSubmitButton"),
                isLoading = isLoading,
                loadingIndicatorTag = "SignUpLoadingIndicator",
                enabled = isFormValid
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onNavigateToLogin,
                enabled = !isLoading
            ) {
                Text(
                    "RETURN TO AUTHORIZATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ValidationMessage(message: String, visible: Boolean) {
    if (!visible) return

    Text(
        text = message,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}
