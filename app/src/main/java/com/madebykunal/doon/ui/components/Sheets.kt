package com.madebykunal.doon.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.madebykunal.doon.state.Nav
import com.madebykunal.doon.state.SheetEntry
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.HIT_TARGET
import com.madebykunal.doon.ui.theme.RADIUS_DOCK
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val LocalSheetClose = staticCompositionLocalOf<() -> Unit> { {} }
val LocalSheetTall = staticCompositionLocalOf { false }

@Composable
fun SheetHost(content: @Composable (SheetEntry) -> Unit) {
    Nav.sheets.toList().forEach { entry ->
        key(entry.key) {
            val state = entry.visible
            LaunchedEffect(state.isIdle, state.currentState) {
                if (state.isIdle && !state.currentState && !state.targetState) Nav.removeSheet(entry)
            }
            AnimatedVisibility(state, enter = EnterTransition.None, exit = ExitTransition.None) {
                val close = remember(entry) { { entry.visible.targetState = false } }
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.fillMaxSize().animateEnterExit(fadeIn(tween(160)), fadeOut(tween(160)))
                            .background(Color.Black.copy(alpha = 0.32f)).tap(role = null, label = "Close", onClick = close),
                    )
                    Box(
                        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(Modifier.animateEnterExit(slideInVertically(tween(220)) { it }, slideOutVertically(tween(180)) { it })) {
                            CompositionLocalProvider(LocalSheetClose provides close) { content(entry) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SheetFrame(
    title: String,
    tall: Boolean = false,
    action: Triple<String, Boolean, () -> Unit>? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = colors
    val close = LocalSheetClose.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val dismissAt = with(LocalDensity.current) { 120.dp.toPx() }
    val drag = rememberDraggableState { delta -> scope.launch { offset.snapTo(maxOf(0f, offset.value + delta)) } }
    var m = Modifier.widthIn(max = 640.dp).fillMaxWidth()
    m = if (tall) m.fillMaxHeight(0.95f) else m.heightIn(max = 720.dp)
    Column(
        m.offset { IntOffset(0, offset.value.roundToInt()) }
            .clip(RoundedCornerShape(topStart = RADIUS_DOCK, topEnd = RADIUS_DOCK))
            .background(c.surface),
    ) {
        Column(
            Modifier.fillMaxWidth().draggable(
                drag,
                Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (offset.value > dismissAt || velocity > 1500f) close()
                    else scope.launch { offset.animateTo(0f, tween(160)) }
                },
            ),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = Space.s), contentAlignment = Alignment.Center) {
                Box(Modifier.size(36.dp, 4.dp).clip(CircleShape).background(c.inkMuted.copy(alpha = 0.35f)))
            }
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = GUTTER), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.widthIn(min = 64.dp).heightIn(min = HIT_TARGET).tap(label = "Cancel", onClick = close), contentAlignment = Alignment.CenterStart) {
                    Txt("Cancel", Type.secondary, c.inkMuted)
                }
                Txt(title, Type.secondaryStrong, modifier = Modifier.weight(1f), maxLines = 1, align = TextAlign.Center)
                Box(Modifier.widthIn(min = 64.dp).heightIn(min = HIT_TARGET), contentAlignment = Alignment.CenterEnd) {
                    if (action != null) {
                        val (label, enabled, run) = action
                        Txt(label, Type.secondaryStrong, if (enabled) c.accentText else c.inkMuted, Modifier.tap(enabled = enabled, label = label, onClick = run))
                    }
                }
            }
        }
        Column(Modifier.weight(1f, fill = tall).verticalScroll(rememberScrollState())) {
            content()
            Spacer(Modifier.navigationBarsPadding().height(Space.xxl))
        }
    }
}
