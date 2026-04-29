package com.example.carousel.carousel

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

data class CarouselItemTransform(
    val alpha: Float = 1f,
    val scale: Float = 1f,
    val rotationZ: Float = 0f,
    val rotationY: Float = 0f,
    val zIndex: Float = 0f,
    val visible: Boolean = true,
)

object MagneticCarouselDefaults {
    val snapSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    val fadeScaleTransform: (Float) -> CarouselItemTransform = { progress ->
        val distance = abs(progress)
        CarouselItemTransform(
            alpha = (1f - distance * 0.18f).coerceIn(0f, 1f),
            scale = (1f - distance * 0.08f).coerceIn(0.58f, 1.12f),
            rotationZ = progress * -4f,
            zIndex = 100f - distance,
            visible = distance <= 4.8f,
        )
    }

    val discTransform: (Float) -> CarouselItemTransform = { progress ->
        val distance = abs(progress)
        CarouselItemTransform(
            alpha = (1f - distance * 0.22f).coerceIn(0f, 1f),
            scale = (1f - distance * 0.07f).coerceIn(0.62f, 1f),
            rotationZ = progress * 9f,
            rotationY = progress * -7f,
            zIndex = 100f - distance,
            visible = distance <= 3.7f,
        )
    }
}

internal fun CarouselItemTransform.withFade(
    visibleSideCards: Int,
    stepPercent: Float,
    progress: Float,
): CarouselItemTransform {
    val ring = abs(progress).roundToInt()
    val fadeStep = (stepPercent / 100f).coerceIn(0f, 1f)
    val tunedAlpha = when {
        ring == 0 -> 1f
        ring > visibleSideCards -> 0f
        else -> fadeStep.pow(ring)
    }
    return copy(alpha = alpha * tunedAlpha, visible = visible && ring <= visibleSideCards + 1)
}
