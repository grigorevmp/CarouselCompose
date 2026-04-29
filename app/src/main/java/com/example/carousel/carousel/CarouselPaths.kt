package com.example.carousel.carousel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

fun interface CarouselPath {
    fun position(progress: Float, container: Size, item: Size, density: Density): Offset
}

data class FractionalOffset(val x: Float, val y: Float)

class LineCarouselPath(
    private val angleDegrees: Float = 0f,
    private val spacing: Dp = 128.dp,
    private val anchor: FractionalOffset = FractionalOffset(0.5f, 0.55f),
) : CarouselPath {
    override fun position(progress: Float, container: Size, item: Size, density: Density): Offset {
        val angle = angleDegrees.toRadians()
        val distance = with(density) { spacing.toPx() } * progress
        val center = Offset(container.width * anchor.x, container.height * anchor.y)
        return Offset(
            x = center.x + cos(angle) * distance - item.width / 2f,
            y = center.y + sin(angle) * distance - item.height / 2f,
        )
    }
}

class ArcCarouselPath(
    private val radius: Dp = 168.dp,
    private val degreesPerItem: Float = 24f,
    private val zeroAngleDegrees: Float = -90f,
    private val center: FractionalOffset = FractionalOffset(0.5f, 0.98f),
) : CarouselPath {
    override fun position(progress: Float, container: Size, item: Size, density: Density): Offset {
        val angle = (zeroAngleDegrees + progress * degreesPerItem).toRadians()
        val radiusPx = with(density) { radius.toPx() }
        val origin = Offset(container.width * center.x, container.height * center.y)
        return Offset(
            x = origin.x + cos(angle) * radiusPx - item.width / 2f,
            y = origin.y + sin(angle) * radiusPx - item.height / 2f,
        )
    }
}

class MorphCarouselPath(
    private val first: CarouselPath,
    private val second: CarouselPath,
    private val secondWeight: Float,
) : CarouselPath {
    override fun position(progress: Float, container: Size, item: Size, density: Density): Offset {
        val weight = secondWeight.coerceIn(0f, 1f)
        val a = first.position(progress, container, item, density)
        val b = second.position(progress, container, item, density)
        return Offset(
            x = lerp(a.x, b.x, weight),
            y = lerp(a.y, b.y, weight),
        )
    }
}
