package com.aura.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Animated pulsing "AI core" HUD element — the Iron-Man-inspired visual
 * anchor of the app. Pulses faster/brighter while [active] (listening or
 * speaking), idles as a slow breathing glow otherwise.
 */
@Composable
fun CoreOrb(active: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "core-pulse")

    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (active) 500 else 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val ringRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier.size(120.dp)) {
        val radius = size.minDimension / 2.4f * pulse
        val center = Offset(size.width / 2, size.height / 2)

        // Outer rotating HUD ring
        rotate(degrees = ringRotation, pivot = center) {
            drawCircle(
                brush = Brush.sweepGradient(listOf(secondary, primary, secondary)),
                radius = radius * 1.4f,
                center = center,
                style = Stroke(width = 3f)
            )
        }

        // Core glow
        drawCircle(
            brush = Brush.radialGradient(listOf(primary, primary.copy(alpha = 0.1f))),
            radius = radius,
            center = center
        )
    }
}
