package com.example.carousel.demo

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

@Composable
internal fun animateDragFloat(target: Float, enabled: Boolean, label: String): Float {
    if (!enabled) return 0f
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = label,
    )
    return animated
}

@Composable
internal fun animateDragAlpha(target: Float, enabled: Boolean, label: String): Float {
    if (!enabled) return target
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(120),
        label = label,
    )
    return animated
}
