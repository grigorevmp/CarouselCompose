package com.example.carousel.carousel

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun <T> InfiniteMagneticCarousel(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: MagneticCarouselState = rememberMagneticCarouselState(),
    itemSize: DpSize = DpSize(92.dp, 128.dp),
    visibleItemRadius: Int = 8,
    path: CarouselPath = LineCarouselPath(),
    gestureConfig: CarouselGestureConfig = CarouselGestureConfig(),
    transform: (progress: Float) -> CarouselItemTransform = MagneticCarouselDefaults.fadeScaleTransform,
    animateItemPlacement: Boolean = false,
    userScrollEnabled: Boolean = true,
    placementProgress: @Composable (dataIndex: Int, virtualIndex: Int, progress: Float) -> Float = { _, _, progress -> progress },
    itemModifier: @Composable (dataIndex: Int, virtualIndex: Int, progress: Float, visualOffset: Offset) -> Modifier = { _, _, _, _ -> Modifier },
    onIndexChanged: (Int) -> Unit = {},
    itemContent: @Composable (item: T, dataIndex: Int, progress: Float) -> Unit,
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val animationScope = rememberCoroutineScope()
    val itemSizePx = with(density) { Size(itemSize.width.toPx(), itemSize.height.toPx()) }
    var animationJob by remember { mutableStateOf<Job?>(null) }
    var width by remember { mutableIntStateOf(0) }
    var height by remember { mutableIntStateOf(0) }
    val scrollUnitPx = with(density) { max(1f, gestureConfig.scrollUnit.toPx()) }

    LaunchedEffect(items.size, state) {
        snapshotFlow { floorMod(state.currentIndex, items.size) }
            .distinctUntilChanged()
            .collect(onIndexChanged)
    }

    Box(
        modifier = modifier
            .onSizeChanged {
                width = it.width
                height = it.height
            }
            .pointerInput(scrollUnitPx, gestureConfig, state, userScrollEnabled) {
                if (!userScrollEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    animationJob?.cancel()
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var lastPosition = down.position
                    var pointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.first()
                        pointerId = change.id
                        if (!change.pressed) break

                        val delta = change.positionChange()
                        if (delta != Offset.Zero) {
                            change.consume()
                            lastPosition = change.position
                            tracker.addPosition(change.uptimeMillis, lastPosition)
                            val indexDelta = delta.x / scrollUnitPx * gestureConfig.dragResistance
                            state.position -= indexDelta
                        }
                    }

                    val velocity = tracker.calculateVelocity()
                    val indexVelocity = -velocity.x / scrollUnitPx
                    animationJob = animationScope.launch {
                        state.flingAndSnap(indexVelocity, gestureConfig)
                    }
                }
            },
    ) {
        val container = Size(width.toFloat(), height.toFloat())
        val current = state.position
        val center = current.roundToInt()
        val virtualItems = (center - visibleItemRadius..center + visibleItemRadius)
            .map { virtualIndex ->
                val progress = virtualIndex - current
                val dataIndex = floorMod(virtualIndex, items.size)
                val visualProgress = placementProgress(dataIndex, virtualIndex, progress)
                val itemTransform = transform(visualProgress)
                val renderTransform = when {
                    animateItemPlacement && itemTransform.visible -> itemTransform.copy(alpha = itemTransform.alpha.coerceAtLeast(0.38f))
                    animateItemPlacement -> itemTransform.copy(alpha = 0f, visible = true)
                    else -> itemTransform
                }
                RenderedCarouselItem(virtualIndex, progress, visualProgress, renderTransform)
            }
            .filter { it.transform.visible && (animateItemPlacement || it.transform.alpha > 0.01f) }
            .sortedBy { abs(it.progress) }
            .distinctBy { floorMod(it.virtualIndex, items.size) }
            .sortedBy { it.transform.zIndex }

        virtualItems.forEach { rendered ->
            val dataIndex = floorMod(rendered.virtualIndex, items.size)
            val item = items[dataIndex]
            val offset = path.position(rendered.visualProgress, container, itemSizePx, density)
            key(item.hashCode()) {
                val layerX = animatePlacementSpring(offset.x, animateItemPlacement, "item-x")
                val layerY = animatePlacementSpring(offset.y, animateItemPlacement, "item-y")
                val layerAlpha = animatePlacementTween(rendered.transform.alpha, animateItemPlacement, "item-alpha")
                val layerScale = animatePlacementSpring(rendered.transform.scale, animateItemPlacement, "item-scale")
                Box(
                    modifier = Modifier
                        .size(itemSize)
                        .graphicsLayer {
                            translationX = layerX
                            translationY = layerY
                            alpha = layerAlpha
                            scaleX = layerScale
                            scaleY = layerScale
                            rotationZ = rendered.transform.rotationZ
                            rotationY = rendered.transform.rotationY
                            cameraDistance = 16f * density.density
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(itemModifier(dataIndex, rendered.virtualIndex, rendered.progress, Offset(layerX, layerY))),
                    ) {
                        itemContent(item, dataIndex, rendered.visualProgress)
                    }
                }
            }
        }
    }
}

private data class RenderedCarouselItem(
    val virtualIndex: Int,
    val progress: Float,
    val visualProgress: Float,
    val transform: CarouselItemTransform,
)

@Composable
private fun animatePlacementSpring(target: Float, enabled: Boolean, label: String): Float {
    if (!enabled) return target
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = label,
    )
    return animated
}

@Composable
private fun animatePlacementTween(target: Float, enabled: Boolean, label: String): Float {
    if (!enabled) return target
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(180),
        label = label,
    )
    return animated
}
