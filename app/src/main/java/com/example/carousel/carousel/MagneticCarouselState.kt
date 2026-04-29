package com.example.carousel.carousel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Stable
class MagneticCarouselState(initialIndex: Int = 0) {
    internal var position by mutableFloatStateOf(initialIndex.toFloat())

    val currentIndex: Int
        get() = position.roundToInt()

    suspend fun scrollTo(index: Int) {
        position = index.toFloat()
    }

    fun snapToLoopIndex(dataIndex: Int, itemCount: Int) {
        if (itemCount <= 0) return
        position = (stableLoopStart(itemCount) + dataIndex.coerceIn(0, itemCount - 1)).toFloat()
    }

    suspend fun animateTo(index: Int) {
        val animatable = Animatable(position)
        animatable.animateTo(index.toFloat(), MagneticCarouselDefaults.snapSpec) {
            position = value
        }
    }
}

@Composable
fun rememberMagneticCarouselState(initialIndex: Int = 0): MagneticCarouselState {
    return remember { MagneticCarouselState(initialIndex) }
}

data class CarouselGestureConfig(
    val scrollUnit: Dp = 116.dp,
    val maxFlingItems: Float = 6f,
    val flingFriction: Float = 2.2f,
    val dragResistance: Float = 1f,
    val snapDampingRatio: Float = Spring.DampingRatioMediumBouncy,
    val snapStiffness: Float = Spring.StiffnessMediumLow,
)

internal suspend fun MagneticCarouselState.flingAndSnap(
    initialVelocity: Float,
    config: CarouselGestureConfig,
) {
    val animatable = Animatable(position)
    val clampedVelocity = initialVelocity.coerceIn(
        -config.maxFlingItems * 8f,
        config.maxFlingItems * 8f,
    )
    animatable.animateDecay(
        initialVelocity = clampedVelocity,
        animationSpec = exponentialDecay(frictionMultiplier = config.flingFriction),
    ) {
        position = value
    }
    animatable.animateTo(
        targetValue = animatable.value.roundToInt().toFloat(),
        animationSpec = spring(
            dampingRatio = config.snapDampingRatio,
            stiffness = config.snapStiffness,
        ),
    ) {
        position = value
    }
}
