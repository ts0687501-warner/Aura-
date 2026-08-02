package com.aura.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuraColors = darkColorScheme(
    primary = Color(0xFF3FD0F5),      // holographic cyan-blue
    secondary = Color(0xFF0B79D0),
    background = Color(0xFF000814),   // near-black navy
    surface = Color(0xFF001D3D),
    onPrimary = Color(0xFF001220),
    onBackground = Color(0xFFCFF4FF),
    onSurface = Color(0xFFCFF4FF)
)

@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuraColors,
        content = content
    )
}
