package com.madebykunal.doon.ui.components

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madebykunal.doon.state.DialogAction
import com.madebykunal.doon.state.Dialogs
import com.madebykunal.doon.state.Nav
import com.madebykunal.doon.state.Route
import com.madebykunal.doon.state.Snack
import com.madebykunal.doon.ui.theme.CHECKBOX_SIZE
import com.madebykunal.doon.ui.theme.DOCK_HEIGHT
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.HIT_TARGET
import com.madebykunal.doon.ui.theme.RADIUS_CARD
import com.madebykunal.doon.ui.theme.RADIUS_DOCK
import com.madebykunal.doon.ui.theme.ROW_MIN_HEIGHT
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import kotlinx.coroutines.delay

val tabular = "tnum"

@Composable
fun Txt(
    text: String,
    style: TextStyle = Type.body,
    color: Color = colors.ink,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    tabularNums: Boolean = false,
    unscaled: Boolean = false,
) {
    val density = LocalDensity.current
    var s = style.copy(color = color, textAlign = align ?: TextAlign.Unspecified)
    if (tabularNums) s = s.copy(fontFeatureSettings = tabular)
    if (unscaled && density.fontScale != 1f) {
        s = s.copy(fontSize = (style.fontSize.value / density.fontScale).sp, lineHeight = (style.lineHeight.value / density.fontScale).sp)
    }
    BasicText(text, modifier, s, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tap(
    enabled: Boolean = true,
    pressedAlpha: Float? = null,
    pressedColor: Color? = null,
    role: Role? = Role.Button,
    label: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    var m: Modifier = this
    if (pressedColor != null && pressed) m = m.background(pressedColor)
    if (pressedAlpha != null) m = m.alpha(if (pressed) pressedAlpha else 1f)
    m.combinedClickable(
        interactionSource = source,
        indication = null,
        enabled = enabled,
        role = role,
        onClickLabel = label,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

fun Modifier.blockTaps(): Modifier = pointerInput(Unit) { detectTapGestures { } }

fun Modifier.hairlineBottom(color: Color): Modifier = drawBehind {
    val w = 1.dp.toPx()
    drawLine(color, Offset(0f, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
}

fun Modifier.hairlineTop(color: Color): Modifier = drawBehind {
    drawLine(color, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1f)
}

@Composable
fun HeaderButton(ic: Ic, label: String, size: Dp = 20.dp, onClick: () -> Unit) {
    Box(Modifier.size(HIT_TARGET).tap(pressedAlpha = 0.5f, label = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(ic, colors.inkMuted, size)
    }
}

@Composable
fun SettingsButton() = HeaderButton(Ic.Menu, "Menu", 24.dp) { Nav.push(Route.Settings) }

@Composable
fun ScreenHeader(title: String, back: Boolean = false, onTitle: (() -> Unit)? = null, right: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(top = Space.s, bottom = Space.m).padding(horizontal = GUTTER),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (back) {
            Box(Modifier.offset(x = -Space.m).size(HIT_TARGET).tap(label = "Back") { Nav.popScreen() }, contentAlignment = Alignment.Center) {
                Icon(Ic.ChevronLeft, colors.ink, 24.dp)
            }
        }
        Txt(title.uppercase(), Type.display, maxLines = 1, modifier = Modifier.weight(1f).then(if (onTitle != null) Modifier.tap(label = "Edit", onClick = onTitle) else Modifier))
        Row(content = right)
    }
}

@Composable
fun Toggle(value: Boolean) {
    val p by animateFloatAsState(if (value) 1f else 0f, tween(180), label = "toggle")
    val off = colors.inkMuted.copy(alpha = 0.3f)
    val on = colors.accent
    val track = lerp(off, on, p)
    Box(Modifier.size(46.dp, 28.dp).clip(RectangleShape).background(track).padding(3.dp)) {
        Box(Modifier.offset(x = 18.dp * p).size(22.dp).shadow(2.dp, RectangleShape).background(Color.White, RectangleShape))
    }
}

private val checkPath: Path by lazy { parsePath("M6.2 11.4 L9.7 14.8 L15.9 7.9") }

@Composable
fun Checkbox(checked: Boolean, onToggle: () -> Unit) {
    val progress = remember { Animatable(if (checked) 1f else 0f) }
    LaunchedEffect(checked) { progress.animateTo(if (checked) 1f else 0f, tween(160)) }
    val c = colors
    Box(Modifier.size(HIT_TARGET).tap(role = Role.Checkbox, onClick = onToggle), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(CHECKBOX_SIZE)) {
            val unit = size.width / 22f
            val corner = CornerRadius(6.dp.toPx())
            drawRoundRect(c.accent.copy(alpha = if (c.dark) 0.28f else 0.18f), cornerRadius = corner)
            val p = progress.value
            if (p > 0f) {
                val side = size.width * minOf(1f, p * 1.6f)
                drawRoundRect(c.accent, Offset((size.width - side) / 2, (size.height - side) / 2), Size(side, side), corner)
                val t = maxOf(0f, p - 0.35f) / 0.65f
                if (t > 0f) {
                    val measure = PathMeasure()
                    measure.setPath(checkPath, false)
                    val segment = Path()
                    measure.getSegment(0f, measure.length * t, segment, true)
                    scale(unit, unit, Offset.Zero) {
                        drawPath(segment, c.onAccent, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.clip(RectangleShape).background(colors.accentTint).padding(horizontal = Space.m, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Type.body,
    singleLine: Boolean = true,
    minHeight: Dp = 0.dp,
    autoFocus: Boolean = false,
    capitalize: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onSubmit: (() -> Unit)? = null,
    maxLength: Int = Int.MAX_VALUE,
) {
    val c = colors
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BasicTextField(
        value = value,
        onValueChange = { onChange(it.take(maxLength)) },
        modifier = modifier.focusRequester(focus).border(1.dp, c.hairline, RectangleShape).heightIn(min = minHeight).padding(horizontal = Space.m, vertical = 10.dp),
        textStyle = style.copy(color = c.ink),
        singleLine = singleLine,
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(
            capitalization = if (capitalize) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            imeAction = if (singleLine) imeAction else ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }, onSearch = { onSubmit?.invoke() }),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Txt(placeholder, style, c.inkMuted)
                inner()
            }
        },
    )
}

@Composable
fun SectionLabel(text: String) {
    Txt(text, Type.captionStrong, colors.inkMuted, Modifier.padding(start = GUTTER, end = GUTTER, top = Space.xl, bottom = Space.s))
}

@Composable
fun OptionRow(
    label: String,
    detail: String? = null,
    icon: (@Composable () -> Unit)? = null,
    right: (@Composable () -> Unit)? = null,
    destructive: Boolean = false,
    selected: Boolean? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = colors
    var m = Modifier.fillMaxWidth()
    if (onClick != null) m = m.tap(pressedColor = c.hairline, onClick = onClick)
    Row(
        m.heightIn(min = ROW_MIN_HEIGHT).hairlineBottom(c.hairline).padding(horizontal = GUTTER, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        icon?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(label, color = if (destructive) c.dangerText else if (selected == true) c.accentText else c.ink, maxLines = 2)
            if (detail != null) Txt(detail, Type.caption, c.inkMuted, maxLines = 2)
        }
        right?.invoke()
    }
}

@Composable
fun SwitchOptionRow(label: String, value: Boolean, detail: String? = null, onChange: (Boolean) -> Unit) {
    OptionRow(label, detail, right = { Toggle(value) }, onClick = { onChange(!value) })
}

@Composable
fun Odometer(value: String, style: TextStyle, color: Color = colors.ink) {
    val density = LocalDensity.current
    val s = style.copy(fontSize = (style.fontSize.value / density.fontScale).sp, lineHeight = (style.lineHeight.value / density.fontScale).sp)
    val lineHeight = with(density) { s.lineHeight.toDp() }
    Row(Modifier.height(lineHeight)) {
        value.forEachIndexed { i, char ->
            androidx.compose.runtime.key(value.length - i) {
                OdometerColumn(char, s, color, lineHeight)
            }
        }
    }
}

@Composable
private fun OdometerColumn(char: Char, style: TextStyle, color: Color, lineHeight: Dp) {
    val digit = char.digitToIntOrNull()
    if (digit == null) {
        Txt(char.toString(), style, color)
        return
    }
    val y = remember { Animatable(digit.toFloat()) }
    LaunchedEffect(digit) { y.animateTo(digit.toFloat(), tween(130)) }
    val textStyle = style.copy(color = color, fontFeatureSettings = tabular, textAlign = TextAlign.Center)
    Box(Modifier.height(lineHeight).clipToBounds()) {
        BasicText("0", Modifier.alpha(0f), textStyle)
        Column(Modifier.wrapContentHeight(Alignment.Top, unbounded = true).offset(y = -lineHeight * y.value)) {
            for (d in 0..9) BasicText(d.toString(), Modifier.height(lineHeight), textStyle)
        }
    }
}

@Composable
fun DialogButton(action: DialogAction, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = colors
    val (fill, ink) = when (action.tone) {
        "primary" -> c.accent to c.onAccent
        "destructive" -> c.danger to c.onDanger
        "cancel" -> c.hairline to c.ink
        else -> c.accentTint to c.accentText
    }
    Box(
        modifier.heightIn(min = 46.dp).clip(RectangleShape).background(fill).tap(pressedAlpha = 0.8f, onClick = onClick).padding(horizontal = Space.l),
        contentAlignment = Alignment.Center,
    ) {
        Txt(action.label, Type.secondaryStrong, ink, maxLines = 1)
    }
}

@Composable
fun DialogShell(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = colors
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).tap(role = null, label = "Dismiss", onClick = onDismiss).padding(Space.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 360.dp).fillMaxWidth().clip(RectangleShape).background(c.surface)
                .border(1.dp, c.hairline, RectangleShape).blockTaps().padding(Space.xxl),
            content = content,
        )
    }
}

@Composable
fun DialogHost() {
    val dialog = Dialogs.current
    var last by remember { mutableStateOf(dialog) }
    if (dialog != null) last = dialog
    AnimatedVisibility(dialog != null, enter = fadeIn(tween(110)), exit = fadeOut(tween(110))) {
        val d = last ?: return@AnimatedVisibility
        DialogShell(Dialogs::dismiss) {
            Txt(d.title, Type.h2)
            if (d.message != null) Txt(d.message, Type.secondary, colors.inkMuted, Modifier.padding(top = Space.s))
            val stacked = d.actions.size > 2
            val buttons = @Composable { mod: Modifier ->
                d.actions.forEach { action ->
                    DialogButton(action, mod) {
                        Dialogs.dismiss()
                        action.onPress?.invoke()
                    }
                }
            }
            Spacer(Modifier.height(Space.xl))
            if (stacked) Column(verticalArrangement = Arrangement.spacedBy(Space.s)) { buttons(Modifier.fillMaxWidth()) }
            else Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) { buttons(Modifier.weight(1f)) }
        }
    }
}

@Composable
fun SnackbarHost(bottom: Dp) {
    val c = colors
    val snack = Snack.current
    var last by remember { mutableStateOf(snack) }
    if (snack != null) last = snack
    LaunchedEffect(snack?.id) {
        val id = snack?.id ?: return@LaunchedEffect
        delay(Snack.UNDO_WINDOW_MS)
        Snack.dismiss(id)
    }
    Box(Modifier.fillMaxSize().padding(start = Space.l, end = Space.l, bottom = bottom), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(snack != null, enter = fadeIn(tween(160)) + slideInVertically(tween(160)) { it / 2 }, exit = fadeOut(tween(160)) + slideOutVertically(tween(160)) { it / 2 }) {
            val s = last ?: return@AnimatedVisibility
            val bg = if (c.dark) c.ink else Color(0xFF26262A)
            val fg = if (c.dark) c.bg else Color.White
            Row(
                Modifier.fillMaxWidth().clip(RectangleShape).background(bg).padding(horizontal = Space.l, vertical = Space.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.l),
            ) {
                Txt(s.message, Type.secondary, fg, Modifier.weight(1f), maxLines = 2)
                val undo = s.onUndo
                if (undo != null) {
                    Txt("Undo", Type.secondaryStrong, fg, Modifier.tap(label = "Undo") {
                        if (System.currentTimeMillis() < s.undoUntil) undo()
                        Snack.dismiss(s.id)
                    })
                }
            }
        }
    }
}

@Composable
fun dockInset(): Dp {
    return 96.dp
}

@Composable
fun EmojiTile(emoji: String, size: Dp, radius: Dp, modifier: Modifier = Modifier, style: TextStyle = Type.h2) {
    Box(modifier.size(size).clip(RectangleShape).background(colors.accentTint), contentAlignment = Alignment.Center) {
        Txt(emoji, style, unscaled = true)
    }
}

@Composable
fun EmptyState(title: String, body: String) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = GUTTER * 2).padding(bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(Space.s, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Txt(title, Type.h2, align = TextAlign.Center)
        Txt(body, Type.secondary, colors.inkMuted, align = TextAlign.Center)
    }
}
