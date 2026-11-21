package com.zaeemtechnologies.ai.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Zaeem {
    val Background = Color(0xFF03070D); val Surface = Color(0xFF0A1B2E); val Elevated = Color(0xFF0D2540)
    val Border = Color(0xFF1C5FA8); val Azure = Color(0xFF0D6BFF); val Ice = Color(0xFF39A9EB)
    val Secondary = Color(0xFFC0C7D1); val Text = Color(0xFFF3F6FA); val Muted = Color(0xFF8695A5)
    val Warning = Color(0xFFE4BC78); val OnAzure = Color(0xFFFFFFFF); val Ok = Color(0xFF2ECC71)
}

@Composable
fun ZaeemTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        background = Zaeem.Background, surface = Zaeem.Surface, surfaceVariant = Zaeem.Elevated,
        primary = Zaeem.Azure, onPrimary = Zaeem.OnAzure, onBackground = Zaeem.Text,
        onSurface = Zaeem.Text, outline = Zaeem.Border, secondary = Zaeem.Ice),
    content = content)
