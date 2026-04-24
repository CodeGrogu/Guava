package com.codegrogu.guava.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import com.codegrogu.guava.model.UserRole
import com.codegrogu.guava.ui.components.GarageBackground
import com.codegrogu.guava.ui.components.GarageButton
import com.codegrogu.guava.ui.components.GarageTextField
import com.codegrogu.guava.ui.components.RoleCard
import com.codegrogu.guava.ui.theme.IndustrialBlack

@Composable
fun SignUpScreen(
    onSignUp: (String, String, String, UserRole) -> Unit,
    isLoading: Boolean = false,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.MECHANIC) }

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
                leadingIcon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(16.dp))

            GarageTextField(
                value = email,
                onValueChange = { email = it },
                label = "Comms Address (Email)",
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            GarageTextField(
                value = password,
                onValueChange = { password = it },
                label = "Access Code",
                leadingIcon = Icons.Default.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
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
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                RoleCard(
                    title = "Manager",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    selected = role == UserRole.MANAGER,
                    onClick = { role = UserRole.MANAGER },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            GarageButton(
                text = "Initialize",
                onClick = { onSignUp(email, password, name, role) },
                isLoading = isLoading,
                enabled = email.isNotBlank() && password.isNotBlank() && name.isNotBlank()
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
                    color = IndustrialBlack
                )
            }
        }
    }
}
