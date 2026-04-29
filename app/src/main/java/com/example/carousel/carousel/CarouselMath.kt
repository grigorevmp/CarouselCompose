package com.example.carousel.carousel

import kotlin.math.PI

internal fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}

internal fun Float.toRadians(): Float = (this / 180f * PI).toFloat()

internal fun floorMod(value: Int, size: Int): Int = ((value % size) + size) % size

fun stableLoopStart(itemCount: Int): Int {
    return itemCount * 1_000
}
