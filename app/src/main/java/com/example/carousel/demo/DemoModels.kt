package com.example.carousel.demo

import androidx.compose.ui.graphics.Color

internal data class DemoCard(val number: Int, val color: Color) {
    val title: String = "#${number.toString().padStart(2, '0')}"
}

internal val demoColors = listOf(
    Color(0xFFFF6B6B),
    Color(0xFFFFB86B),
    Color(0xFF66E3A7),
    Color(0xFF4DD0E1),
    Color(0xFF7C5CFF),
    Color(0xFFFF6FCF),
)

internal fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from !in indices || to !in indices || from == to) return
    val item = removeAt(from)
    add(to, item)
}
