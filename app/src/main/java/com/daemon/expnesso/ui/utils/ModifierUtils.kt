package com.daemon.expnesso.ui.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.daemon.expnesso.ui.theme.PremiumSurface
import com.daemon.expnesso.ui.theme.PremiumSurfaceVariant

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    val colors = listOf(
        PremiumSurface,
        PremiumSurfaceVariant,
        PremiumSurface
    )

    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(10f, 10f),
        end = Offset(translateAnim, translateAnim)
    )

    this.background(brush)
}
