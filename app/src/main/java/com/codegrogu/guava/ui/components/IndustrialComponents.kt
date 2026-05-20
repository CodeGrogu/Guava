package com.codegrogu.guava.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codegrogu.guava.ui.theme.IndustrialBlack
import com.codegrogu.guava.ui.theme.SafetyOrange

@Composable
fun GarageBackground(modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val gridColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
    
    Canvas(modifier = modifier.fillMaxSize().background(backgroundColor)) {
        val step = 40.dp.toPx()
        
        // Vertical lines
        for (x in 0..(size.width / step).toInt()) {
            drawLine(
                color = gridColor,
                start = Offset(x * step, 0f),
                end = Offset(x * step, size.height),
                strokeWidth = 1f
            )
        }
        
        // Horizontal lines
        for (y in 0..(size.height / step).toInt()) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y * step),
                end = Offset(size.width, y * step),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
fun GarageButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingIndicatorTag: String? = null,
    color: Color = SafetyOrange
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = MaterialTheme.colorScheme.onBackground == Color.White
    
    val offset by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 4.dp,
        label = "ButtonOffset"
    )

    val shadowColor = if (isDark) Color.Black else IndustrialBlack
    val borderColor = if (isDark) Color.White.copy(alpha = 0.5f) else IndustrialBlack
    val contentColor = IndustrialBlack // Buttons usually have dark text on bright orange/gray

    Box(
        modifier = modifier
            .padding(bottom = 4.dp, end = 4.dp)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(shadowColor)
        )
        
        // Main Button Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset(x = -offset + 4.dp, y = -offset + 4.dp)
                .border(2.dp, borderColor),
            color = if (enabled) color else Color.Gray,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .then(
                                if (loadingIndicatorTag != null) {
                                    Modifier.testTag(loadingIndicatorTag)
                                } else {
                                    Modifier
                                }
                            ),
                        color = contentColor,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(
                        text = text.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = contentColor
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun GarageTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    fieldTag: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (fieldTag != null) {
                        Modifier.testTag(fieldTag)
                    } else {
                        Modifier
                    }
                ),
            enabled = enabled,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = true,
            shape = MaterialTheme.shapes.extraSmall, // More geometric
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
fun RoleCard(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.onBackground == Color.White
    val elevation by animateDpAsState(if (selected) 0.dp else 4.dp)
    val color by animateColorAsState(if (selected) SafetyOrange else MaterialTheme.colorScheme.surface)
    
    val shadowColor = if (isDark) Color.Black else IndustrialBlack
    val borderColor = if (isDark && !selected) Color.White.copy(alpha = 0.3f) else IndustrialBlack
    val contentColor = if (selected) IndustrialBlack else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .padding(4.dp)
            .clickable(onClick = onClick)
    ) {
        // Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(shadowColor)
        )
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .offset(x = -elevation + 4.dp, y = -elevation + 4.dp)
                .border(2.dp, borderColor),
            color = color
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                )
            }
        }
    }
}
