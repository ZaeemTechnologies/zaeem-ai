package com.zaeemtechnologies.ai.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Zaeem {
    val Background = Color(0xFF080C11); val Surface = Color(0xFF101820); val Elevated = Color(0xFF1B2633)
    val Border = Color(0xFF314355); val Azure = Color(0xFF39A9EB); val Ice = Color(0xFF79C5F4)
    val Secondary = Color(0xFFA4B1BF); val Text = Color(0xFFF3F6FA); val Muted = Color(0xFF8695A5)
    val Warning = Color(0xFFE4BC78); val OnAzure = Color(0xFF080C11)
}

@Composable
fun ZaeemTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        background = Zaeem.Background, surface = Zaeem.Surface, surfaceVariant = Zaeem.Elevated,
        primary = Zaeem.Azure, onPrimary = Zaeem.OnAzure, onBackground = Zaeem.Text,
        onSurface = Zaeem.Text, outline = Zaeem.Border, secondary = Zaeem.Ice),
    content = content)
