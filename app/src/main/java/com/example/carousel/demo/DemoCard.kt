package com.example.carousel.demo

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
internal fun DemoCarouselCard(
    card: DemoCard,
    progress: Float,
    jitterStrength: Float,
    verticalJitter: Boolean,
) {
    val selected = abs(progress) < 0.5f
    val infiniteTransition = rememberInfiniteTransition(label = "card-jitter-${card.number}")
    val jitterPhase by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900 + card.number * 37),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "jitter-phase",
    )
    val jitterWeight = (1f - abs(progress) * 0.16f).coerceIn(0f, 1f)
    val jitterModifier = Modifier.graphicsLayer {
        translationX = jitterPhase * jitterStrength * jitterWeight
        translationY = if (verticalJitter) -jitterPhase * jitterStrength * 0.45f * jitterWeight else 0f
        rotationZ = jitterPhase * jitterStrength * 0.08f * jitterWeight
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(card.color, card.color.copy(alpha = 0.55f), Color(0xFF13182C)),
                ),
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.22f),
                shape = RoundedCornerShape(28.dp),
            )
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .then(jitterModifier)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
                .align(Alignment.TopEnd),
        )
        Text(
            text = card.title,
            color = Color.White,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .align(Alignment.Center)
                .then(jitterModifier),
        )
        Text(
            text = "p=${String.format("%.1f", progress)}",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(jitterModifier),
        )
    }
}
