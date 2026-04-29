package com.example.carousel.demo

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun ModeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF7C5CFF) else Color(0xFF202944),
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(text = text)
    }
}

@Composable
internal fun SettingSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueText: String,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = valueText,
                color = Color(0xFFAFC0FF),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.height(28.dp),
        )
    }
}

@Composable
internal fun DragTrashTarget(visible: Boolean, active: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, animationSpec = tween(180), label = "trash-alpha")
    val scale by animateFloatAsState(
        targetValue = when {
            !visible -> 0.82f
            active -> 1.12f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "trash-scale",
    )
    if (alpha <= 0.01f) return

    Surface(
        color = if (active) Color(0xFFFF4D6D) else Color(0xFF222C4A),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .padding(bottom = 18.dp)
            .width(190.dp)
            .height(62.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (active) 0.9f else 0.22f),
                shape = RoundedCornerShape(28.dp),
            ),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = if (active) "Отпустить в корзину" else "Корзина",
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
        }
    }
}
