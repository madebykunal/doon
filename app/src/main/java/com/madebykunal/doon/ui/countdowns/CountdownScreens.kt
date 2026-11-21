package com.madebykunal.doon.ui.countdowns

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madebykunal.doon.data.Countdown
import com.madebykunal.doon.data.CountdownRepo
import com.madebykunal.doon.data.Live
import com.madebykunal.doon.data.T
import com.madebykunal.doon.lib.CountdownMath
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.parseTime
import com.madebykunal.doon.notify.Reminders
import com.madebykunal.doon.state.DialogAction
import com.madebykunal.doon.state.Dialogs
import com.madebykunal.doon.state.Nav
import com.madebykunal.doon.state.Route
import com.madebykunal.doon.state.SheetRoute
import com.madebykunal.doon.state.Snack
import com.madebykunal.doon.state.Ui
import com.madebykunal.doon.ui.components.EmojiTile
import com.madebykunal.doon.ui.components.EmptyState
import com.madebykunal.doon.ui.components.HeaderButton
import com.madebykunal.doon.ui.components.Ic
import com.madebykunal.doon.ui.components.Icon
import com.madebykunal.doon.ui.components.Odometer
import com.madebykunal.doon.ui.components.ScreenHeader
import com.madebykunal.doon.ui.components.SearchButton
import com.madebykunal.doon.ui.components.SettingsButton
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.dockInset
import com.madebykunal.doon.ui.components.tap
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.HIT_TARGET
import com.madebykunal.doon.ui.theme.RADIUS_DOCK
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import kotlinx.coroutines.delay
import java.text.NumberFormat
import kotlin.math.ceil

@Composable
fun CountdownsScreen() {
    val c = colors
    val today = Ui.today
    val version = Live.version(T.COUNTDOWNS)
    val data = remember(version) { CountdownRepo.all().filter { it.archivedAt == null } }
    var pastOpen by remember { mutableStateOf(false) }
    val upcoming = CountdownMath.sort(data.filter { !CountdownMath.isPast(it.dates, today) }, today) { it.dates }
    val past = data.filter { CountdownMath.isPast(it.dates, today) }.sortedByDescending { it.targetDate }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Countdowns") {
            SearchButton()
            SettingsButton()
        }
        if (upcoming.isEmpty() && past.isEmpty()) {
            EmptyState("Nothing to look forward to yet?", "Add a birthday, a trip, or the day you started something.")
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = dockInset())) {
            items(upcoming, key = { it.id }) { CountdownCard(it, today) }
            if (past.isNotEmpty()) {
                item("past") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = Space.s).heightIn(min = HIT_TARGET).tap(label = "Past, ${past.size}") { pastOpen = !pastOpen }.padding(horizontal = GUTTER),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.xs),
                    ) {
                        Txt("Past · ${past.size}", Type.secondaryStrong, c.inkMuted)
                        Icon(if (pastOpen) Ic.ChevronDown else Ic.ChevronRight, c.inkMuted, 16.dp)
                    }
                }
                if (pastOpen) items(past, key = { it.id }) { CountdownCard(it, today) }
            }
        }
    }
}

@Composable
private fun CountdownCard(item: Countdown, today: String) {
    val c = colors
    val display = CountdownMath.display(item.dates, today)
    val title = CountdownMath.titleWithAge(item.dates, item.title, today)
    val date = CountdownMath.dateLine(item.dates, today) + if (item.targetTime != null && item.kind == "until") " · ${item.targetTime}" else ""
    Row(
        Modifier.fillMaxWidth().padding(horizontal = GUTTER - Space.xs).padding(bottom = Space.s)
            .clip(RoundedCornerShape(RADIUS_DOCK - Space.xs)).background(c.surface).border(1.dp, c.hairline, RoundedCornerShape(RADIUS_DOCK - Space.xs))
            .tap(pressedAlpha = 0.85f, label = title) { Nav.push(Route.CountdownDetail(item.id)) }
            .padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        EmojiTile(item.emoji, 48.dp, 24.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(title, Type.bodyStrong, maxLines = 1)
            Txt(date, Type.caption, c.inkMuted, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Txt(display.value, if (display.numeric) Type.title else Type.secondaryStrong, c.accentText, maxLines = 1, tabularNums = true)
            if (display.unit.isNotEmpty()) Txt(display.unit.split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }, Type.micro, c.accentText)
        }
    }
}

val EMOJI_STYLE = Type.title.copy(fontSize = 40.sp, lineHeight = 52.sp)

private fun plural(n: Int, word: String) = "${NumberFormat.getIntegerInstance().format(n)} $word${if (n == 1) "" else "s"}"

@Composable
fun CountdownDetailScreen(id: String) {
    val c = colors
    val today = Ui.today
    val version = Live.version(T.COUNTDOWNS)
    val item = remember(version, id) { CountdownRepo.get(id) }
    if (item == null || item.deletedAt != null) {
        ScreenHeader("", back = true)
        return
    }
    val isToday = item.kind != "since" && CountdownMath.daysUntil(item.dates, today) == 0
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isToday, item.targetTime) {
        if (!isToday || item.targetTime == null) return@LaunchedEffect
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            now = System.currentTimeMillis()
        }
    }
    val timeLeft = if (isToday && item.targetTime != null) {
        val (h, m) = parseTime(item.targetTime)
        val left = Dates.toMillis(today, h, m) - now
        if (left <= 0) "Now" else {
            val minutes = ceil(left / 60_000.0).toInt()
            val hours = minutes / 60
            if (hours > 0) "in $hours h ${minutes % 60} min" else "in $minutes min"
        }
    } else null
    val display = CountdownMath.display(item.dates, today)
    val grid = CountdownMath.dotGrid(item.dates, Dates.dateOfMillis(item.createdAt), today)
    val countingUp = item.kind == "since"
    val caption = when {
        countingUp -> "${plural(grid.elapsedDays, "day")} so far"
        grid.remainingDays == 0 -> "${plural(grid.elapsedDays, "day")} gone"
        else -> "${plural(grid.elapsedDays, "day")} gone, ${plural(grid.remainingDays, "day")} to go"
    }
    val meaning = if (grid.unit == "day") "" else ". Each dot is a ${grid.unit}."

    val remove = {
        Dialogs.show(
            "Delete this countdown?",
            item.title,
            listOf(
                DialogAction("Cancel", "cancel"),
                DialogAction("Delete", "destructive") {
                    CountdownRepo.softDelete(id)
                    Reminders.syncCountdown(id)
                    Nav.popScreen()
                    Snack.show("Countdown deleted") {
                        CountdownRepo.restore(id)
                        Reminders.syncCountdown(id)
                    }
                },
            ),
        )
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("", back = true) {
            HeaderButton(Ic.Pencil, "Edit countdown", 18.dp) { Nav.openSheet(SheetRoute.CountdownEditor(id)) }
            HeaderButton(Ic.Trash, "Delete countdown", 18.dp, remove)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = GUTTER), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Txt(item.emoji, EMOJI_STYLE, unscaled = true)
            Txt(CountdownMath.titleWithAge(item.dates, item.title, today), Type.title)
            Txt(CountdownMath.dateLine(item.dates, today) + if (item.targetTime != null && item.kind == "until") " · ${item.targetTime}" else "", Type.secondary, c.inkMuted)
            Spacer(Modifier.height(Space.xl))
            CountRow(display.value, display.unit, display.numeric)
            if (timeLeft != null) Txt(timeLeft, Type.secondaryStrong, c.accentText)
            BoxWithConstraints(Modifier.padding(top = Space.xxl, bottom = Space.s)) {
                DotMatrix(maxWidth, grid.total, grid.filled, !countingUp)
            }
            Txt(caption + meaning, Type.caption, c.inkMuted)
            Spacer(Modifier.height(dockInset()))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountRow(value: String, unit: String, numeric: Boolean) {
    val c = colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.Bottom, itemVerticalAlignment = Alignment.Bottom) {
        if (numeric) Odometer(value, Type.hero) else Txt(value, Type.numeral)
        if (unit.isNotEmpty()) Txt(unit, Type.secondary, c.inkMuted, Modifier.padding(bottom = Space.m))
    }
}

@Composable
private fun DotMatrix(width: Dp, total: Int, filled: Int, showNow: Boolean) {
    val c = colors
    val pitchTarget = if (total <= 60) 26.dp else if (total <= 160) 18.dp else 13.dp
    val columns = maxOf(1, (width / pitchTarget).toInt())
    val pitch = width / columns
    val height = pitch * ceil(total / columns.toDouble()).toInt()
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(500)) }
    Box(Modifier.fillMaxWidth().height(height)) {
        Canvas(Modifier.fillMaxSize()) {
            val p = size.width / columns
            val dot = p * 0.56f
            for (i in filled until total) {
                drawCircle(c.inkMuted.copy(alpha = 0.28f), dot / 2, Offset((i % columns) * p + p / 2, (i / columns) * p + p / 2))
            }
        }
        Canvas(Modifier.fillMaxWidth().height(height * reveal.value).clipToBounds()) {
            val p = size.width / columns
            val dot = p * 0.56f
            for (i in 0 until filled) {
                drawCircle(c.accent, dot / 2, Offset((i % columns) * p + p / 2, (i / columns) * p + p / 2))
            }
            if (showNow && filled < total) {
                drawCircle(c.ink, dot / 2, Offset((filled % columns) * p + p / 2, (filled / columns) * p + p / 2))
            }
        }
    }
}
