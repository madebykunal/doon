package com.madebykunal.doon.ui.components

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.madebykunal.doon.state.Nav
import com.madebykunal.doon.ui.theme.DOCK_HEIGHT
import com.madebykunal.doon.ui.theme.DOCK_SIDE
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.colors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

enum class NavKind { Today, Lists, Countdowns, Habits, Plus }

private val tabs = listOf(NavKind.Today to "Today", NavKind.Lists to "Lists", NavKind.Countdowns to "Countdowns", NavKind.Habits to "Habits")

private val todayPaths by lazy { listOf("M7.5 5h9a4 4 0 0 1 4 4v7.5a4 4 0 0 1-4 4h-9a4 4 0 0 1-4-4V9a4 4 0 0 1 4-4z", "M8 3v4M16 3v4M3.5 10h17").map(::parsePath) }
private val hourglassPaths by lazy { listOf("M6 3h12M6 21h12", "M7.5 3c0 4.5 4.5 6 4.5 9s-4.5 4.5-4.5 9M16.5 3c0 4.5-4.5 6-4.5 9s4.5 4.5 4.5 9").map(::parsePath) }
private val hourglassFill by lazy { parsePath("M9.25 4.25h5.5L12 7.25zM9.25 19.75 12 16.75l2.75 3z") }
private val habitStroke by lazy { listOf("M12 21v-8.5M8 21h8", "M12 13.5C12 9.6 9.4 7 5 7c0 3.9 2.6 6.5 7 6.5Z").map(::parsePath) }
private val habitLeaf by lazy { parsePath("M12 11c0-3.9 2.6-6.5 7-6.5 0 3.9-2.6 6.5-7 6.5Z") }

private fun reducedMotion(context: android.content.Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

@Composable
fun NavIcon(kind: NavKind, color: Color, size: Dp, progress: Float) {
    val bump = sin(progress * PI).toFloat()
    Canvas(
        Modifier.size(size).graphicsLayer {
            when (kind) {
                NavKind.Today -> {
                    cameraDistance = 8 * density
                    rotationX = progress * 360f
                }
                NavKind.Countdowns -> rotationZ = progress * 180f
                NavKind.Habits -> {
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    scaleX = 1 + 0.18f * bump
                    scaleY = 1 + 0.18f * bump
                    rotationZ = sin(progress * PI * 2).toFloat() * 12f
                }
                NavKind.Plus -> {
                    rotationZ = progress * 90f
                    scaleX = 1 + 0.12f * bump
                    scaleY = 1 + 0.12f * bump
                }
                NavKind.Lists -> translationX = -2 * bump * density
            }
        },
    ) {
        val s = this.size.width / 24f
        scale(s, s, Offset.Zero) { glyph(kind, color, progress) }
    }
}

private fun DrawScope.glyph(kind: NavKind, color: Color, progress: Float) {
    val stroke = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (kind) {
        NavKind.Today -> {
            todayPaths.forEach { drawPath(it, color, style = stroke) }
            drawCircle(color, 1.9f, Offset(12f, 15.25f))
        }
        NavKind.Lists -> {
            listOf(7f, 12f, 17f).forEachIndexed { i, y ->
                drawCircle(color, 1.5f, Offset(5f, y))
                val delay = i * 0.15f
                val local = if (progress >= 1f) 1f else ((progress - delay) / 0.5f).coerceIn(0f, 1f)
                val length = if (i == 2) 6.5f else 10f
                drawLine(color, Offset(9.5f, y), Offset(9.5f + length * local, y), 2f, StrokeCap.Round)
            }
        }
        NavKind.Countdowns -> {
            hourglassPaths.forEach { drawPath(it, color, style = stroke) }
            drawPath(hourglassFill, color)
        }
        NavKind.Habits -> {
            habitStroke.forEach { drawPath(it, color, style = stroke) }
            drawPath(habitLeaf, color)
            drawPath(habitLeaf, color, style = stroke)
        }
        NavKind.Plus -> {
            val plus = Stroke(2.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(12f, 5f), Offset(12f, 19f), plus.width, StrokeCap.Round)
            drawLine(color, Offset(5f, 12f), Offset(19f, 12f), plus.width, StrokeCap.Round)
        }
    }
}

@Composable
fun Dock(visible: Boolean, onPlus: () -> Unit) {
    val c = colors
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(160), label = "dock")
    if (alpha == 0f) return
    val context = LocalContext.current
    val reduced = remember { reducedMotion(context) }
    val scope = rememberCoroutineScope()
    val plusSpin = remember { Animatable(1f) }
    val shadowColor = Color.Black.copy(alpha = if (c.dark) 0.4f else 0.08f)
    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = Space.s).padding(horizontal = DOCK_SIDE),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier.fillMaxWidth().height(DOCK_HEIGHT).graphicsLayer {
                this.alpha = alpha
                translationY = (1 - alpha) * 80 * density
            },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Island(0, 1, Modifier.weight(1f), reduced, shadowColor)
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) 0.88f else 1f, tween(if (pressed) 110 else 160), label = "plus")
            Box(
                Modifier.size(DOCK_HEIGHT).graphicsLayer { scaleX = scale; scaleY = scale }
                    .shadow(8.dp, CircleShape, ambientColor = shadowColor, spotColor = shadowColor)
                    .clip(CircleShape).background(c.accent)
                    .semantics { contentDescription = "Add" }
                    .clickable(interactionSource = source, indication = null, role = Role.Button) {
                        if (!reduced) scope.launch {
                            plusSpin.snapTo(0f)
                            plusSpin.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
                        }
                        onPlus()
                    },
                contentAlignment = Alignment.Center,
            ) {
                NavIcon(NavKind.Plus, c.onAccent, 26.dp, plusSpin.value)
            }
            Island(2, 3, Modifier.weight(1f), reduced, shadowColor)
        }
    }
}

@Composable
private fun Island(first: Int, last: Int, modifier: Modifier, reduced: Boolean, shadowColor: Color) {
    val c = colors
    Row(
        modifier.height(DOCK_HEIGHT).shadow(8.dp, CircleShape, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(CircleShape).background(c.surface).border(1.dp, c.hairline, CircleShape).padding(horizontal = 6.dp),
    ) {
        for (index in first..last) TabButton(index, Modifier.weight(1f), reduced)
    }
}

@Composable
private fun TabButton(index: Int, modifier: Modifier, reduced: Boolean) {
    val c = colors
    val (kind, label) = tabs[index]
    val selected = Nav.tab == index
    val near by animateFloatAsState(if (selected) 1f else 0f, tween(200), label = "tab")
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.88f else 1f, tween(if (pressed) 110 else 160), label = "press")
    val play = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val iconColor by animateColorAsState(if (selected) c.onAccent else c.inkMuted, tween(200), label = "icon")
    Box(
        modifier.height(DOCK_HEIGHT).selectable(selected, interactionSource = source, indication = null, role = Role.Tab) {
            if (!reduced) scope.launch {
                play.snapTo(0f)
                play.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
            }
            if (Nav.tab == index) Nav.stacks[index].clear()
            Nav.selectTab(index)
        }.semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(DOCK_HEIGHT - 12.dp).graphicsLayer {
                alpha = near
                val s = (0.55f + 0.45f * near) * press
                scaleX = s
                scaleY = s
            }.clip(CircleShape).background(c.accent),
        )
        Box(Modifier.graphicsLayer { scaleX = press; scaleY = press }) {
            NavIcon(kind, iconColor, 24.dp, play.value)
        }
    }
}
