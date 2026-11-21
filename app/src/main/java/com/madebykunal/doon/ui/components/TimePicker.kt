package com.madebykunal.doon.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madebykunal.doon.lib.pad2
import com.madebykunal.doon.lib.parseTime
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val DIAL = 248.dp
private val KNOB = 40.dp
private const val OUTER = 0.82f
private const val INNER = 0.56f

private fun hourSlot(hour: Int): Pair<Int, Boolean> = when (hour) {
    0 -> 0 to false
    12 -> 0 to true
    in 1..11 -> hour to true
    else -> hour - 12 to false
}

private fun hourAt(slot: Int, outer: Boolean): Int = if (outer) (if (slot == 0) 12 else slot) else (if (slot == 0) 0 else slot + 12)

@Composable
fun TimePicker(value: String, onChange: (String) -> Unit) {
    val c = colors
    val (hour, minute) = parseTime(value)
    var minutes by remember { mutableStateOf(false) }
    val current by rememberUpdatedState(hour to minute)
    val change by rememberUpdatedState(onChange)

    Column(Modifier.fillMaxWidth().padding(vertical = Space.s), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            Segment(pad2(hour), !minutes, "Hour ${pad2(hour)}") { minutes = false }
            Txt(":", Type.numeral.copy(fontSize = 44.sp, lineHeight = 52.sp), c.inkMuted, unscaled = true)
            Segment(pad2(minute), minutes, "Minute ${pad2(minute)}") { minutes = true }
        }

        val angle = if (minutes) minute * 6f else (hourSlot(hour).first % 12) * 30f
        val radius = if (minutes) OUTER else if (hourSlot(hour).second) OUTER else INNER
        val unwrapped = remember { floatArrayOf(angle) }
        unwrapped[0] += ((angle - unwrapped[0]) % 360 + 540) % 360 - 180
        val animatedAngle by animateFloatAsState(unwrapped[0], tween(120), label = "hand")
        val animatedRadius by animateFloatAsState(radius, tween(120), label = "ring")
        val accent = c.accent
        val onAccent = c.onAccent
        val face = c.hairline

        Box(
            Modifier.padding(top = Space.m).size(DIAL).semantics { contentDescription = if (minutes) "Minute dial" else "Hour dial" }
                .pointerInput(minutes) {
                    awaitEachGesture {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val half = size.width / 2f
                        fun pick(p: Offset) {
                            val dx = p.x - center.x
                            val dy = p.y - center.y
                            val deg = ((Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())) + 360) % 360).toFloat()
                            val (h, m) = current
                            if (minutes) {
                                val next = (deg / 6f).roundToInt() % 60
                                if (next != m) change("${pad2(h)}:${pad2(next)}")
                            } else {
                                val outer = sqrt(dx * dx + dy * dy) > half * (OUTER + INNER) / 2
                                val next = hourAt((deg / 30f).roundToInt() % 12, outer)
                                if (next != h) change("${pad2(next)}:${pad2(m)}")
                            }
                        }
                        val down = awaitFirstDown()
                        down.consume()
                        pick(down.position)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                            event.changes.firstOrNull()?.let { pick(it.position) }
                        } while (event.changes.any { it.pressed })
                        if (!minutes) minutes = true
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(face, size.width / 2, center)
                val rad = Math.toRadians(animatedAngle.toDouble())
                val r = size.width / 2 * animatedRadius
                val tip = Offset(center.x + (sin(rad) * r).toFloat(), center.y - (cos(rad) * r).toFloat())
                drawLine(accent, center, tip, 2.dp.toPx())
                drawCircle(accent, 4.dp.toPx(), center)
                drawCircle(accent, KNOB.toPx() / 2, tip)
                if (minutes && minute % 5 != 0) drawCircle(onAccent, 3.dp.toPx(), tip)
            }
            if (minutes) {
                Ring((0 until 12).map { pad2(it * 5) }, OUTER, if (minute % 5 == 0) minute / 5 else -1, Type.secondaryStrong)
            } else {
                val (slot, outer) = hourSlot(hour)
                Ring((0 until 12).map { if (it == 0) "12" else it.toString() }, OUTER, if (outer) slot else -1, Type.secondaryStrong)
                Ring((0 until 12).map { if (it == 0) "00" else (it + 12).toString() }, INNER, if (!outer) slot else -1, Type.caption)
            }
        }
    }
}

@Composable
private fun Segment(text: String, selected: Boolean, label: String, onClick: () -> Unit) {
    val c = colors
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) c.accentTint else c.hairline)
            .tap(label = label, onClick = onClick).padding(horizontal = Space.m, vertical = Space.xs),
        contentAlignment = Alignment.Center,
    ) {
        Txt(text, Type.numeral.copy(fontSize = 44.sp, lineHeight = 52.sp), if (selected) c.accentText else c.ink, tabularNums = true, unscaled = true)
    }
}

@Composable
private fun Ring(labels: List<String>, ratio: Float, selected: Int, style: androidx.compose.ui.text.TextStyle) {
    val c = colors
    Layout(
        content = {
            labels.forEachIndexed { i, label ->
                Box(Modifier.size(KNOB), contentAlignment = Alignment.Center) {
                    Txt(label, style, if (i == selected) c.onAccent else if (ratio < OUTER) c.inkMuted else c.ink, tabularNums = true, unscaled = true)
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            val cx = constraints.maxWidth / 2f
            val cy = constraints.maxHeight / 2f
            val r = constraints.maxWidth / 2f * ratio
            placeables.forEachIndexed { i, p ->
                val a = i * PI / 6
                val x = cx + (sin(a) * r).toFloat() - p.width / 2f
                val y = cy - (cos(a) * r).toFloat() - p.height / 2f
                p.place(x.roundToInt(), y.roundToInt())
            }
        }
    }
}
