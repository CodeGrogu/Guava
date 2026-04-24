package com.codegrogu.guava.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codegrogu.guava.ui.components.GarageBackground
import com.codegrogu.guava.ui.components.GarageButton
import com.codegrogu.guava.ui.components.GarageTextField
import com.codegrogu.guava.ui.theme.SafetyOrange

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit,
    isLoading: Boolean = false,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        GarageBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // "Logo"
            Surface(
                modifier = Modifier.size(80.dp),
                color = MaterialTheme.colorScheme.onBackground,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "G",
                        color = SafetyOrange,
                        style = MaterialTheme.typography.displayMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "VALENTINE'S GARAGE",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "SYSTEM ACCESS v2.8",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            GarageTextField(
                value = email,
                onValueChange = { email = it },
                label = "Terminal ID (Email)",
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            GarageTextField(
                value = password,
                onValueChange = { password = it },
                label = "Passcode",
                leadingIcon = Icons.Default.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(48.dp))

            GarageButton(
                text = "Authorize",
                onClick = { onLoginSuccess(email, password) },
                isLoading = isLoading,
                enabled = email.isNotBlank() && password.isNotBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onNavigateToSignUp,
                enabled = !isLoading
            ) {
                Text(
                    "INITIALIZE NEW ACCOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
