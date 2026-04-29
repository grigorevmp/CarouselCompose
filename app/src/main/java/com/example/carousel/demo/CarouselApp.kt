package com.example.carousel.demo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.carousel.carousel.ArcCarouselPath
import com.example.carousel.carousel.CarouselGestureConfig
import com.example.carousel.carousel.CarouselItemTransform
import com.example.carousel.carousel.FractionalOffset
import com.example.carousel.carousel.InfiniteMagneticCarousel
import com.example.carousel.carousel.LineCarouselPath
import com.example.carousel.carousel.MagneticCarouselDefaults
import com.example.carousel.carousel.MorphCarouselPath
import com.example.carousel.carousel.floorMod
import com.example.carousel.carousel.rememberMagneticCarouselState
import com.example.carousel.carousel.stableLoopStart
import com.example.carousel.carousel.withFade
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CarouselApp() {
    val cards = remember { List(17) { DemoCard(it + 1, demoColors[it % demoColors.size]) }.toMutableStateList() }
    val state = rememberMagneticCarouselState(stableLoopStart(cards.size))
    var mode by remember { mutableIntStateOf(1) }
    var selected by remember { mutableIntStateOf(0) }
    var curvature by remember { mutableFloatStateOf(0.55f) }
    var straightness by remember { mutableFloatStateOf(0.35f) }
    var spacing by remember { mutableFloatStateOf(112f) }
    var jitter by remember { mutableFloatStateOf(2.4f) }
    var fadeVisibleSideCards by remember { mutableFloatStateOf(4f) }
    var fadeStepPercent by remember { mutableFloatStateOf(70f) }
    var stickyBounce by remember { mutableFloatStateOf(0.65f) }
    var smoothFollow by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var draggedVirtualIndex by remember { mutableIntStateOf(Int.MIN_VALUE) }
    var draggedCard by remember { mutableStateOf<DemoCard?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragTopLeft by remember { mutableStateOf(Offset.Zero) }
    var carouselWidth by remember { mutableIntStateOf(1) }
    var carouselHeight by remember { mutableIntStateOf(1) }
    var nextCardNumber by remember { mutableIntStateOf(cards.maxOf { it.number } + 1) }

    val density = LocalDensity.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val animationScope = rememberCoroutineScope()
    val spacingDp = spacing.dp
    val isDragging = draggedIndex != -1 && draggedCard != null
    val displayCards = cards.toList()
    val cardWidthPx = with(density) { 94.dp.toPx() }
    val cardHeightPx = with(density) { 132.dp.toPx() }
    val spacingPx = with(density) { spacing.dp.toPx() }
    val overTrash = isDragging && dragTopLeft.y + cardHeightPx / 2f > carouselHeight - with(density) { 92.dp.toPx() }
    val dropRadius = minOf(9, cards.size / 2)
    val rawProjectedDropVirtualIndex = if (isDragging) {
        val floatingCenterX = dragTopLeft.x + cardWidthPx / 2f
        val slotFromCenter = ((floatingCenterX - carouselWidth / 2f) / spacingPx).roundToInt()
        state.currentIndex + slotFromCenter
    } else {
        Int.MIN_VALUE
    }
    val projectedDropVirtualIndex = if (isDragging) {
        rawProjectedDropVirtualIndex.coerceIn(state.currentIndex - dropRadius, state.currentIndex + dropRadius)
    } else {
        Int.MIN_VALUE
    }
    val projectedDropIndex = if (isDragging) floorMod(projectedDropVirtualIndex, cards.size) else -1
    val latestDraggedIndex by rememberUpdatedState(draggedIndex)
    val latestProjectedDropIndex by rememberUpdatedState(projectedDropIndex)
    val latestOverTrash by rememberUpdatedState(overTrash)

    LaunchedEffect(state) {
        snapshotFlow { state.currentIndex }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                vibrateCenterTick(context)
            }
    }

    val straightLine = LineCarouselPath(
        angleDegrees = 0f,
        spacing = spacingDp,
        anchor = FractionalOffset(0.5f, 0.55f),
    )
    val path = when (mode) {
        0 -> straightLine
        1 -> MorphCarouselPath(
            first = straightLine,
            second = ArcCarouselPath(
                radius = (150f + spacing * 0.95f).dp,
                degreesPerItem = 4f + curvature * 38f,
                zeroAngleDegrees = -90f,
                center = FractionalOffset(0.5f, 0.98f),
            ),
            secondWeight = curvature * (1f - straightness),
        )
        else -> MorphCarouselPath(
            first = straightLine,
            second = LineCarouselPath(
                angleDegrees = -36f + curvature * 42f,
                spacing = spacingDp,
                anchor = FractionalOffset(0.5f, 0.58f),
            ),
            secondWeight = 1f - straightness,
        )
    }
    val itemTransform = when (mode) {
        1 -> { progress: Float ->
            MagneticCarouselDefaults.discTransform(progress)
                .withFade(fadeVisibleSideCards.roundToInt(), fadeStepPercent, progress)
        }
        2 -> { progress: Float ->
            val distance = abs(progress)
            CarouselItemTransform(
                alpha = (1f - distance * 0.16f).coerceIn(0f, 1f),
                scale = (1.08f - distance * 0.09f).coerceIn(0.58f, 1.08f),
                rotationZ = progress * 3.5f,
                zIndex = 100f - distance,
                visible = distance <= 5.5f,
            ).withFade(fadeVisibleSideCards.roundToInt(), fadeStepPercent, progress)
        }
        0 -> { progress: Float ->
            val distance = abs(progress)
            CarouselItemTransform(
                alpha = (1f - distance * 0.14f).coerceIn(0f, 1f),
                scale = (1f - distance * 0.06f).coerceIn(0.68f, 1f),
                zIndex = 100f - distance,
                visible = distance <= 5.8f,
            ).withFade(fadeVisibleSideCards.roundToInt(), fadeStepPercent, progress)
        }
        else -> { progress: Float ->
            MagneticCarouselDefaults.fadeScaleTransform(progress)
                .withFade(fadeVisibleSideCards.roundToInt(), fadeStepPercent, progress)
        }
    }

    MaterialTheme {
        Surface(color = Color(0xFF0B1020), modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0B1020), Color(0xFF141B35), Color(0xFF090B12)),
                        ),
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 38.dp, bottom = 24.dp),
            ) {
                Text(
                    text = "Magnetic Infinite Carousel",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Любое число элементов, бесконечная прокрутка, кастомный путь, исчезновение, масштаб, углы и магнит к точке.",
                    color = Color(0xFFC6D0FF),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                        .padding(top = 10.dp)
                        .onSizeChanged {
                            carouselWidth = it.width
                            carouselHeight = it.height
                        }
                        .clipToBounds(),
                ) {
                    InfiniteMagneticCarousel(
                        items = displayCards,
                        state = state,
                        itemSize = DpSize(94.dp, 132.dp),
                        visibleItemRadius = 9,
                        path = path,
                        gestureConfig = CarouselGestureConfig(
                            scrollUnit = (spacing * 0.78f).dp,
                            maxFlingItems = 11f,
                            flingFriction = 1.35f,
                            snapDampingRatio = 0.82f - stickyBounce * 0.52f,
                            snapStiffness = Spring.StiffnessMediumLow + stickyBounce * 520f,
                        ),
                        transform = itemTransform,
                        animateItemPlacement = smoothFollow || isDragging,
                        userScrollEnabled = !isDragging,
                        placementProgress = { _, virtualIndex, progress ->
                            when {
                                !isDragging || overTrash -> progress
                                projectedDropVirtualIndex > draggedVirtualIndex && virtualIndex in (draggedVirtualIndex + 1)..projectedDropVirtualIndex -> progress - 1f
                                projectedDropVirtualIndex < draggedVirtualIndex && virtualIndex in projectedDropVirtualIndex until draggedVirtualIndex -> progress + 1f
                                else -> progress
                            }
                        },
                        itemModifier = { dataIndex, virtualIndex, progress, visualOffset ->
                            val itemIsDragged = cards.getOrNull(dataIndex) == draggedCard
                            val originalAlpha = animateDragAlpha(
                                target = if (isDragging && itemIsDragged) 0f else 1f,
                                enabled = isDragging,
                                label = "drag-source-alpha",
                            )
                            Modifier
                                .graphicsLayer {
                                    alpha *= originalAlpha
                                }
                                .pointerInput(virtualIndex) {
                                    detectTapGestures(
                                        onTap = {
                                            if (abs(progress) > 0.12f) {
                                                animationScope.launch { state.animateTo(virtualIndex) }
                                            }
                                        },
                                    )
                                }
                                .pointerInput(dataIndex, spacing, cards.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            if (draggedIndex == -1 && cards.size > 1) {
                                                draggedIndex = dataIndex
                                                draggedVirtualIndex = virtualIndex
                                                draggedCard = cards[dataIndex]
                                                dragOffset = Offset.Zero
                                                dragTopLeft = visualOffset
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        onDragCancel = {
                                            draggedIndex = -1
                                            draggedVirtualIndex = Int.MIN_VALUE
                                            draggedCard = null
                                            dragOffset = Offset.Zero
                                            dragTopLeft = Offset.Zero
                                        },
                                        onDragEnd = {
                                            val from = latestDraggedIndex
                                            if (from != -1 && from in cards.indices) {
                                                val to = latestProjectedDropIndex.coerceIn(0, cards.lastIndex)
                                                if (latestOverTrash && cards.size > 1) {
                                                    cards.removeAt(from)
                                                    val nextIndex = from.coerceAtMost(cards.lastIndex)
                                                    state.snapToLoopIndex(nextIndex, cards.size)
                                                    selected = nextIndex
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                } else if (from != to) {
                                                    cards.move(from, to)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            }
                                            draggedIndex = -1
                                            draggedVirtualIndex = Int.MIN_VALUE
                                            draggedCard = null
                                            dragOffset = Offset.Zero
                                            dragTopLeft = Offset.Zero
                                        },
                                        onDrag = { change, amount ->
                                            if (cards.getOrNull(draggedIndex) == draggedCard) {
                                                change.consume()
                                                dragOffset += amount
                                                dragTopLeft = (dragTopLeft + amount).coerceInCarouselBounds(
                                                    maxX = carouselWidth - cardWidthPx,
                                                    maxY = carouselHeight - cardHeightPx,
                                                )
                                            }
                                        },
                                    )
                                }
                        },
                        onIndexChanged = { selected = it },
                        modifier = Modifier.fillMaxSize(),
                    ) { card, _, progress ->
                        DemoCarouselCard(
                            card = card,
                            progress = progress,
                            jitterStrength = jitter,
                            verticalJitter = mode != 0,
                        )
                    }

                    DragTrashTarget(
                        visible = isDragging,
                        active = overTrash,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )

                    draggedCard?.let { card ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(94.dp, 132.dp)
                                .graphicsLayer {
                                    translationX = dragTopLeft.x
                                    translationY = dragTopLeft.y
                                    scaleX = if (overTrash) 0.82f else 1.12f
                                    scaleY = if (overTrash) 0.82f else 1.12f
                                    rotationZ = (dragOffset.x / 42f).coerceIn(-12f, 12f)
                                    alpha = if (overTrash) 0.72f else 0.98f
                                },
                        ) {
                            DemoCarouselCard(
                                card = card,
                                progress = 0f,
                                jitterStrength = 0f,
                                verticalJitter = false,
                            )
                        }
                    }
                }

                Text(
                    text = "Центр: ${cards[selected.coerceIn(0, cards.lastIndex)].title}. Drag and drop: долгий тап, отпусти между карточками или в корзину.",
                    color = Color(0xFFE8ECFF),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )

                DemoSettings(
                    mode = mode,
                    settingsExpanded = settingsExpanded,
                    curvature = curvature,
                    straightness = straightness,
                    spacing = spacing,
                    jitter = jitter,
                    fadeVisibleSideCards = fadeVisibleSideCards,
                    fadeStepPercent = fadeStepPercent,
                    stickyBounce = stickyBounce,
                    smoothFollow = smoothFollow,
                    onModeChange = { mode = it },
                    onSettingsExpandedChange = { settingsExpanded = !settingsExpanded },
                    onCurvatureChange = { curvature = it },
                    onStraightnessChange = { straightness = it },
                    onSpacingChange = { spacing = it },
                    onJitterChange = { jitter = it },
                    onFadeVisibleSideCardsChange = { fadeVisibleSideCards = it.roundToInt().toFloat() },
                    onFadeStepPercentChange = { fadeStepPercent = it },
                    onStickyBounceChange = { stickyBounce = it },
                    onSmoothFollowToggle = { smoothFollow = !smoothFollow },
                    onAddCard = {
                        val insertIndex = selected.coerceIn(0, cards.size) + 1
                        cards.add(insertIndex.coerceAtMost(cards.size), DemoCard(nextCardNumber, demoColors[(nextCardNumber - 1) % demoColors.size]))
                        nextCardNumber += 1
                        state.snapToLoopIndex(insertIndex.coerceAtMost(cards.lastIndex), cards.size)
                        selected = insertIndex.coerceAtMost(cards.lastIndex)
                    },
                )
            }
        }
    }
}

@Composable
private fun DemoModeTabs(mode: Int, onModeChange: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 18.dp),
    ) {
        ModeButton("Линия", mode == 0) { onModeChange(0) }
        ModeButton("Полу-диск", mode == 1) { onModeChange(1) }
        ModeButton("Угол", mode == 2) { onModeChange(2) }
    }
}

@Composable
private fun DemoSettings(
    mode: Int,
    settingsExpanded: Boolean,
    curvature: Float,
    straightness: Float,
    spacing: Float,
    jitter: Float,
    fadeVisibleSideCards: Float,
    fadeStepPercent: Float,
    stickyBounce: Float,
    smoothFollow: Boolean,
    onModeChange: (Int) -> Unit,
    onSettingsExpandedChange: () -> Unit,
    onCurvatureChange: (Float) -> Unit,
    onStraightnessChange: (Float) -> Unit,
    onSpacingChange: (Float) -> Unit,
    onJitterChange: (Float) -> Unit,
    onFadeVisibleSideCardsChange: (Float) -> Unit,
    onFadeStepPercentChange: (Float) -> Unit,
    onStickyBounceChange: (Float) -> Unit,
    onSmoothFollowToggle: () -> Unit,
    onAddCard: () -> Unit,
) {
    Surface(
        color = Color(0xFF151D35).copy(alpha = 0.92f),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .animateContentSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSettingsExpandedChange)
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    text = "Настройки",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (settingsExpanded) "Свернуть" else "Открыть",
                    color = Color(0xFFAFC0FF),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            AnimatedVisibility(
                visible = settingsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DemoModeTabs(mode = mode, onModeChange = onModeChange)
                    SettingSlider(
                        title = "Изогнутость",
                        value = curvature,
                        valueText = "${(curvature * 100).roundToInt()}%",
                        onValueChange = onCurvatureChange,
                    )
                    SettingSlider(
                        title = "Прямость",
                        value = straightness,
                        valueText = when {
                            straightness > 0.96f -> "чистая линия"
                            straightness < 0.04f -> "форма"
                            else -> "${(straightness * 100).roundToInt()}%"
                        },
                        onValueChange = onStraightnessChange,
                    )
                    SettingSlider(
                        title = "Расстояние",
                        value = spacing,
                        valueRange = 76f..170f,
                        valueText = "${spacing.roundToInt()} dp",
                        onValueChange = onSpacingChange,
                    )
                    SettingSlider(
                        title = "Дрожание",
                        value = jitter,
                        valueRange = 0f..8f,
                        valueText = "${String.format("%.1f", jitter)} px",
                        onValueChange = onJitterChange,
                    )
                    SettingSlider(
                        title = "Fade: карточек сбоку",
                        value = fadeVisibleSideCards,
                        valueRange = 0f..9f,
                        valueText = "${fadeVisibleSideCards.roundToInt()} + ${fadeVisibleSideCards.roundToInt()}",
                        onValueChange = onFadeVisibleSideCardsChange,
                    )
                    SettingSlider(
                        title = "Fade: процент шага",
                        value = fadeStepPercent,
                        valueRange = 0f..100f,
                        valueText = "${fadeStepPercent.roundToInt()}%",
                        onValueChange = onFadeStepPercentChange,
                    )
                    SettingSlider(
                        title = "Sticky bounce",
                        value = stickyBounce,
                        valueText = "${(stickyBounce * 100).roundToInt()}%",
                        onValueChange = onStickyBounceChange,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Text(
                            text = "Следование при скролле",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        ModeButton(if (smoothFollow) "Вкл" else "Выкл", smoothFollow, onSmoothFollowToggle)
                    }
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        ModeButton("Добавить карточку", false, onAddCard)
                    }
                }
            }
        }
    }
}

private fun Offset.coerceInCarouselBounds(maxX: Float, maxY: Float): Offset {
    return Offset(
        x = x.coerceIn(0f, maxX.coerceAtLeast(0f)),
        y = y.coerceIn(0f, maxY.coerceAtLeast(0f)),
    )
}
