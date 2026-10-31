package com.athar.mobile.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF1A2E24)
private val Palm = Color(0xFF2F5D4A)
private val Sand = Color(0xFFF3EFE6)
private val Clay = Color(0xFFB56A3A)
private val Mist = Color(0xFFE7E1D4)

private val LightColors = lightColorScheme(
    primary = Palm,
    onPrimary = Color.White,
    secondary = Clay,
    onSecondary = Color.White,
    background = Sand,
    onBackground = Ink,
    surface = Mist,
    onSurface = Ink
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FBFAB),
    onPrimary = Ink,
    secondary = Color(0xFFE0A57A),
    onSecondary = Ink,
    background = Color(0xFF121A16),
    onBackground = Color(0xFFEDE7DB),
    surface = Color(0xFF1C2822),
    onSurface = Color(0xFFEDE7DB)
)

@Composable
fun AtharTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
