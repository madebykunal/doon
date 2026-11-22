package com.madebykunal.doon.ui.todos

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madebykunal.doon.data.Home
import com.madebykunal.doon.data.Todo
import com.madebykunal.doon.data.Where
import com.madebykunal.doon.lib.InlineNode
import com.madebykunal.doon.lib.Markdown
import com.madebykunal.doon.lib.ParseRepeat
import com.madebykunal.doon.state.ComposerState
import com.madebykunal.doon.state.Editor
import com.madebykunal.doon.state.Nav
import com.madebykunal.doon.state.SheetRoute
import com.madebykunal.doon.state.TodoActions
import com.madebykunal.doon.ui.components.Checkbox
import com.madebykunal.doon.ui.components.Chip
import com.madebykunal.doon.ui.components.Ic
import com.madebykunal.doon.ui.components.Icon
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.hairlineBottom
import com.madebykunal.doon.ui.components.hairlineTop
import com.madebykunal.doon.ui.components.tap
import com.madebykunal.doon.ui.theme.CHECKBOX_SIZE
import com.madebykunal.doon.ui.theme.DoonColors
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.HIT_TARGET
import com.madebykunal.doon.ui.theme.ROW_MIN_HEIGHT
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.TOOLBAR_HEIGHT
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

private const val ROLLOVER_BADGE_AT = 3

private fun openDetails(id: String) {
    Editor.commitAll()
    Nav.openSheet(SheetRoute.TodoDetails(id))
}

fun inlineAnnotated(source: String, c: DoonColors): AnnotatedString = buildAnnotatedString {
    fun walk(nodes: List<InlineNode>) {
        nodes.forEach { node ->
            when (node) {
                is InlineNode.Text -> append(node.text)
                is InlineNode.Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp, background = c.hairline)) { append(node.text) }
                is InlineNode.Link -> withLink(LinkAnnotation.Url(node.url, TextLinkStyles(SpanStyle(color = c.accentText, textDecoration = TextDecoration.Underline)))) { append(node.label) }
                is InlineNode.Styled -> withStyle(
                    when (node.style) {
                        InlineNode.Style.Bold -> SpanStyle(fontWeight = FontWeight.SemiBold)
                        InlineNode.Style.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
                        InlineNode.Style.Strike -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    },
                ) { walk(node.children) }
            }
        }
    }
    walk(Markdown.parseInline(source))
}

@Composable
fun InlineText(source: String, style: TextStyle, modifier: Modifier = Modifier, onLayout: ((TextLayoutResult) -> Unit)? = null) {
    val c = colors
    val text = remember(source, c) { inlineAnnotated(source, c) }
    BasicText(text, modifier, style, onTextLayout = onLayout)
}

val TEXT_INSET = GUTTER + 35.dp

@Composable
fun TodoRow(todo: Todo, count: Int, rightLabel: String, onSwipeRight: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = colors
    val highlight = todo.highlight
    val background = if (highlight != null) c.highlightTint(highlight) else c.bg
    SwipeRow(
        id = todo.id,
        enabled = RowDrag.draggingId == null,
        rightLabel = rightLabel,
        onSwipeRight = onSwipeRight,
        asksBeforeDelete = todo.seriesId != null,
        background = background,
        modifier = modifier,
    ) {
        val bar = if (highlight != null) c.highlightBar(highlight) else null
        Row(
            Modifier.fillMaxWidth().hairlineBottom(c.hairline, TEXT_INSET).heightIn(min = 60.dp)
                .drawBehind { if (bar != null) drawRect(bar, size = Size(3.dp.toPx(), size.height)) }
                .padding(start = GUTTER - 11.dp, end = GUTTER),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(todo.completed) { TodoActions.toggle(todo.id) }
            Column(Modifier.weight(1f)) {
                val desc = todo.description
                StrikeText(
                    todo.text,
                    todo.completed,
                    Modifier.fillMaxWidth().tap(label = "Open details") { openDetails(todo.id) }
                        .padding(start = 2.dp, top = 14.dp, bottom = if (desc != null) 2.dp else 14.dp),
                )
                if (desc != null) {
                    Txt(desc, Type.caption, c.inkMuted, Modifier.fillMaxWidth().tap { openDetails(todo.id) }.padding(start = 2.dp, bottom = Space.m), maxLines = 2)
                }
            }
            Row(Modifier.padding(start = Space.s), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (todo.rolloverCount >= ROLLOVER_BADGE_AT) Txt("↻${todo.rolloverCount}", Type.micro, c.inkMuted)
                val remindAt = todo.remindAt
                if (remindAt != null) {
                    Icon(Ic.Bell, c.inkMuted, 13.dp)
                    Txt(com.madebykunal.doon.lib.Dates.formatMillis(remindAt, "HH:mm"), Type.micro, c.inkMuted, tabularNums = true)
                }
                if (todo.seriesId != null) Icon(Ic.Repeat, c.inkMuted, 13.dp)
            }
        }
    }
}

@Composable
private fun StrikeText(source: String, completed: Boolean, modifier: Modifier) {
    val c = colors
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val progress = remember { Animatable(if (completed) 1f else 0f) }
    LaunchedEffect(completed) {
        if (completed) progress.animateTo(1f, tween(280, easing = androidx.compose.animation.core.FastOutSlowInEasing)) else progress.animateTo(0f, tween(160))
    }
    val muted = c.inkMuted
    val ink by androidx.compose.animation.animateColorAsState(if (completed) muted else c.ink, tween(240), label = "todoInk")
    InlineText(
        source,
        Type.body.copy(color = ink, lineHeight = 24.sp),
        modifier.drawWithContent {
            drawContent()
            val l = layout ?: return@drawWithContent
            if (progress.value <= 0f) return@drawWithContent
            val n = l.lineCount
            for (i in 0 until n) {
                val own = (progress.value * n - i).coerceIn(0f, 1f)
                if (own <= 0f) continue
                val left = l.getLineLeft(i)
                val right = l.getLineRight(i)
                val y = (l.getLineTop(i) + l.getLineBottom(i)) / 2
                drawLine(muted, Offset(left, y), Offset(left + (right - left) * own, y), 1.5.dp.toPx())
            }
        },
        onLayout = { layout = it },
    )
}

@Composable
fun SwipeRow(
    id: String,
    enabled: Boolean,
    rightLabel: String,
    onSwipeRight: (String) -> Unit,
    asksBeforeDelete: Boolean,
    background: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = colors
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val collapse = remember { Animatable(1f) }
    var raw by remember { mutableFloatStateOf(0f) }
    val threshold = with(density) { 96.dp.toPx() }
    val band = with(density) { 80.dp.toPx() }
    val screen = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    fun shaped(d: Float): Float {
        val distance = abs(d)
        if (distance <= threshold) return d
        val over = distance - threshold
        return sign(d) * (threshold + band * (1 - 1 / (over / band + 1)))
    }
    val view = androidx.compose.ui.platform.LocalView.current
    var armed by remember { mutableStateOf(false) }
    val drag = rememberDraggableState { delta ->
        raw += delta
        val past = abs(raw) >= threshold
        if (past != armed) {
            armed = past
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
        }
        scope.launch { offset.snapTo(shaped(raw)) }
    }
    Box(
        modifier.layout { measurable, constraints ->
            val p = measurable.measure(constraints)
            val h = (p.height * collapse.value).roundToInt()
            layout(p.width, h) { p.place(0, 0) }
        }.clipToBounds(),
    ) {
        val o = offset.value
        if (o != 0f) {
            val right = o > 0f
            Row(
                Modifier.matchParentSize().background(if (right) c.accent else c.danger).padding(horizontal = GUTTER),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, if (right) Alignment.Start else Alignment.End),
            ) {
                val ink = if (right) c.onAccent else c.onDanger
                val reveal = (kotlin.math.abs(o) / threshold).coerceIn(0f, 1f)
                val grow = if (armed) 1.08f else 1f
                Box(Modifier.graphicsLayer { alpha = reveal; scaleX = grow; scaleY = grow; translationX = (if (right) -1 else 1) * (1 - reveal) * 24.dp.toPx() }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(if (right) Ic.ArrowRight else Ic.Trash, ink, 18.dp)
                        Txt((if (right) rightLabel else "Delete").uppercase(), Type.nav.copy(fontSize = 18.sp, lineHeight = 22.sp), ink, maxLines = 1, unscaled = true)
                    }
                }
            }
        }
        Box(
            Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }.background(background).draggable(
                drag,
                Orientation.Horizontal,
                enabled = enabled,
                onDragStarted = {
                    offset.stop()
                    raw = offset.value
                },
                onDragStopped = { velocity ->
                    val d = raw
                    raw = 0f
                    armed = false
                    val right = d > 0
                    val flung = abs(velocity) > 2000f && sign(velocity) == sign(d) && abs(d) > threshold / 3
                    val settle = spring<Float>(dampingRatio = 0.8f, stiffness = 420f)
                    when {
                        abs(d) < threshold && !flung -> offset.animateTo(0f, settle, initialVelocity = velocity)
                        !right && asksBeforeDelete -> {
                            offset.animateTo(0f, settle, initialVelocity = velocity)
                            TodoActions.delete(id)
                        }
                        else -> {
                            val slide = launch {
                                offset.animateTo(if (right) screen else -screen, tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing), initialVelocity = velocity)
                            }
                            delay(90)
                            collapse.animateTo(0f, tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                            slide.join()
                            if (right) onSwipeRight(id) else TodoActions.delete(id)
                        }
                    }
                },
            ),
        ) { content() }
    }
}

@Composable
fun DragGhost() {
    val c = colors
    if (RowDrag.draggingId == null) return
    val density = LocalDensity.current
    val size = RowDrag.ghostSize
    Box(
        Modifier.offset { IntOffset(RowDrag.ghostX.roundToInt(), RowDrag.ghostY.roundToInt()) }
            .size(with(density) { size.width.toDp() }, with(density) { size.height.toDp() })
            .graphicsLayer { scaleX = 1.02f; scaleY = 1.02f }
            .shadow(10.dp, RectangleShape)
            .background(c.surface, RectangleShape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(Modifier.padding(start = GUTTER, end = GUTTER), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.padding(end = 13.dp).size(CHECKBOX_SIZE).border(1.75.dp, c.inkMuted, RectangleShape))
            Txt(RowDrag.ghostText, Type.body, maxLines = 2)
        }
    }
}
