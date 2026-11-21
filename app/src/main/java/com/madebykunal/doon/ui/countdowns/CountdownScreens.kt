package com.madebykunal.doon.ui.countdowns

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madebykunal.doon.data.Countdown
import com.madebykunal.doon.data.CountdownRepo
import com.madebykunal.doon.data.Live
import com.madebykunal.doon.data.T
import com.madebykunal.doon.lib.CountdownMath
import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.parseTime
import com.madebykunal.doon.notify.Reminders
import com.madebykunal.doon.state.DialogAction
import com.madebykunal.doon.state.Dialogs
import com.madebykunal.doon.state.Nav
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.state.Route
import com.madebykunal.doon.state.SheetRoute
import com.madebykunal.doon.state.Snack
import com.madebykunal.doon.state.Ui
import com.madebykunal.doon.ui.components.EmptyState
import com.madebykunal.doon.ui.components.HeaderButton
import com.madebykunal.doon.ui.components.Ic
import com.madebykunal.doon.ui.components.Odometer
import com.madebykunal.doon.ui.components.ScreenHeader
import com.madebykunal.doon.ui.components.SettingsButton
import com.madebykunal.doon.ui.components.Toggle
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.dockInset
import com.madebykunal.doon.ui.components.hairlineBottom
import com.madebykunal.doon.ui.components.tap
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.Period
import kotlin.math.ceil
import kotlin.math.roundToLong

val EMOJI_STYLE = Type.title.copy(fontSize = 40.sp, lineHeight = 52.sp)

private val BUCKETS = listOf("Next 7 days", "Within a month", "Next 3 months", "Later", "Counting up")

private fun bucketOf(item: Countdown, today: DateStr): String {
    if (item.kind == "since") return "Counting up"
    val days = CountdownMath.daysUntil(item.dates, today)
    return when {
        days <= 7 -> "Next 7 days"
        days <= 31 -> "Within a month"
        days <= 92 -> "Next 3 months"
        else -> "Later"
    }
}

private fun waitProgress(item: Countdown, today: DateStr): Float? {
    if (item.kind == "since" || CountdownMath.isPast(item.dates, today)) return null
    val grid = CountdownMath.dotGrid(item.dates, Dates.dateOfMillis(item.createdAt), today)
    val span = grid.elapsedDays + grid.remainingDays
    return if (span <= 0) 1f else grid.elapsedDays.toFloat() / span
}

private fun confirmDelete(item: Countdown, leave: Boolean) {
    Dialogs.show(
        "Delete this countdown?",
        item.title,
        listOf(
            DialogAction("Delete", "destructive") {
                CountdownRepo.softDelete(item.id)
                Reminders.syncCountdown(item.id)
                if (leave) Nav.popScreen()
                Snack.show("Countdown deleted") {
                    CountdownRepo.restore(item.id)
                    Reminders.syncCountdown(item.id)
                }
            },
            DialogAction("Cancel", "cancel"),
        ),
    )
}

private fun showActions(item: Countdown) {
    Dialogs.show(
        item.title,
        actions = listOf(
            DialogAction("Edit", "primary") { Nav.openSheet(SheetRoute.CountdownEditor(item.id)) },
            DialogAction("Delete", "destructive") { confirmDelete(item, leave = false) },
            DialogAction("Cancel", "cancel"),
        ),
    )
}

@Composable
fun CountdownsScreen() {
    val c = colors
    val today = Ui.today
    val version = Live.version(T.COUNTDOWNS)
    val data = remember(version) { CountdownRepo.all().filter { it.archivedAt == null } }
    var pastOpen by remember { mutableStateOf(false) }
    val upcoming = CountdownMath.sort(data.filter { !CountdownMath.isPast(it.dates, today) }, today) { it.dates }
    val past = if (Prefs.soonShowPast) data.filter { CountdownMath.isPast(it.dates, today) }.sortedByDescending { it.targetDate } else emptyList()
    val next = upcoming.firstOrNull { it.kind != "since" } ?: upcoming.firstOrNull()
    val groups = (upcoming - setOfNotNull(next)).groupBy { bucketOf(it, today) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Soon") { SettingsButton() }
        if (upcoming.isEmpty() && past.isEmpty()) {
            EmptyState("Nothing to look forward to yet?", "Add a birthday, a trip, or the day you started something.")
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = dockInset())) {
            if (next != null) item("next:${next.id}") { NextUp(next, today, Modifier.animateItem()) }
            BUCKETS.forEach { bucket ->
                val rows = groups[bucket] ?: return@forEach
                item("h:$bucket") { GroupLabel(bucket, Modifier.animateItem()) }
                items(rows, key = { it.id }) { CountdownRow(it, today, Modifier.animateItem()) }
            }
            if (past.isNotEmpty()) {
                item("past") {
                    Row(
                        Modifier.animateItem().fillMaxWidth().padding(top = Space.l).heightIn(min = 48.dp).tap(label = "Past, ${past.size}") { pastOpen = !pastOpen }.padding(horizontal = GUTTER),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Txt("PAST", Type.label, c.inkMuted, Modifier.weight(1f))
                        Txt(if (pastOpen) "HIDE" else past.size.toString(), Type.label, c.inkMuted)
                    }
                }
                if (pastOpen) items(past, key = { it.id }) { CountdownRow(it, today, Modifier.animateItem()) }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String, modifier: Modifier) {
    Txt(text.uppercase(), Type.label, colors.inkMuted, modifier.padding(horizontal = GUTTER).padding(top = Space.xxl, bottom = Space.xs))
}

@Composable
private fun NextUp(item: Countdown, today: String, modifier: Modifier) {
    val c = colors
    val display = CountdownMath.display(item.dates, today)
    val progress = waitProgress(item, today)
    val fill = remember(item.id) { Animatable(0f) }
    LaunchedEffect(item.id, progress) { fill.animateTo(progress ?: 0f, tween(900, delayMillis = 120, easing = FastOutSlowInEasing)) }
    Column(
        modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(top = Space.xs)
            .background(c.accent)
            .tap(pressedAlpha = 0.85f, label = item.title, onLongClick = { showActions(item) }) { Nav.push(Route.CountdownDetail(item.id)) }
            .padding(Space.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("NEXT UP", Type.label, c.onAccent.copy(alpha = 0.8f), Modifier.weight(1f))
            if (item.emoji.isNotBlank()) Txt(item.emoji, Type.h2, unscaled = true)
        }
        Row(Modifier.padding(top = Space.m), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt(CountdownMath.titleWithAge(item.dates, item.title, today).uppercase(), Type.display.copy(fontSize = 26.sp, lineHeight = 29.sp), c.onAccent, maxLines = 2)
                Txt(dateText(item, today), Type.caption, c.onAccent.copy(alpha = 0.8f), maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (display.numeric) Odometer(display.value, Type.display.copy(fontSize = 56.sp, lineHeight = 58.sp), c.onAccent)
                else Txt(display.value.removeSuffix(" 🎉").uppercase(), Type.display.copy(fontSize = 30.sp, lineHeight = 34.sp), c.onAccent, maxLines = 1, unscaled = true)
                if (display.unit.isNotEmpty()) Txt(display.unit.uppercase(), Type.label.copy(fontSize = 10.sp), c.onAccent.copy(alpha = 0.8f), unscaled = true)
            }
        }
        if (progress != null) Bar(fill.value, c.onAccent, Modifier.padding(top = Space.l))
    }
}

@Composable
private fun Bar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(3.dp).background(color.copy(alpha = 0.25f))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}

private fun dateText(item: Countdown, today: String): String =
    CountdownMath.dateLine(item.dates, today) + if (item.targetTime != null && item.kind == "until") " · ${item.targetTime}" else ""

@Composable
private fun CountdownRow(item: Countdown, today: String, modifier: Modifier) {
    val c = colors
    val display = CountdownMath.display(item.dates, today)
    val past = CountdownMath.isPast(item.dates, today)
    Row(
        modifier.fillMaxWidth()
            .tap(pressedColor = c.hairline, label = item.title, onLongClick = { showActions(item) }) { Nav.push(Route.CountdownDetail(item.id)) }
            .heightIn(min = 64.dp).padding(horizontal = GUTTER, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        Box(Modifier.widthIn(min = 64.dp)) {
            val value = display.value.removeSuffix(" 🎉").uppercase()
            Txt(
                value,
                Type.display.copy(fontSize = if (display.numeric) 34.sp else 18.sp, lineHeight = 36.sp),
                if (past) c.inkMuted else c.accentText, maxLines = 1, tabularNums = true, unscaled = true,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(CountdownMath.titleWithAge(item.dates, item.title, today), Type.bodyStrong, if (past) c.inkMuted else c.ink, maxLines = 1)
            Txt(dateText(item, today), Type.caption, c.inkMuted, maxLines = 1)
        }
    }
}

private fun whenLabel(target: DateStr, today: DateStr): String {
    val weekStart = Dates.startOfWeek(today, Prefs.weekStart)
    val days = Dates.diffDays(today, target)
    return when {
        days < 0 -> "Passed"
        days == 0 -> "Today"
        days == 1 -> "Tomorrow"
        target < Dates.addDays(weekStart, 7) -> "This week"
        target < Dates.addDays(weekStart, 14) -> "Next week"
        Dates.year(target) == Dates.year(today) && Dates.month(target) == Dates.month(today) -> "This month"
        else -> {
            val months = Period.between(LocalDate.parse(today), LocalDate.parse(target)).toTotalMonths()
            if (months <= 1) "Next month" else "In $months months"
        }
    }
}

private fun breakdown(from: DateStr, to: DateStr): List<Pair<Long, String>> {
    val p = Period.between(LocalDate.parse(from), LocalDate.parse(to))
    val years = p.years.toLong()
    val months = p.months.toLong()
    val weeks = (p.days / 7).toLong()
    val days = (p.days % 7).toLong()
    val cells = mutableListOf<Pair<Long, String>>()
    if (years > 0) cells.add(years to if (years == 1L) "year" else "years")
    if (years > 0 || months > 0) cells.add(months to if (months == 1L) "month" else "months")
    if (years == 0L) cells.add(weeks to if (weeks == 1L) "week" else "weeks")
    cells.add(days to if (days == 1L) "day" else "days")
    return cells.take(3)
}

private class Measure(val label: String, val value: Double, val decimals: Boolean)

private fun measures(value: Long, unit: String): List<Measure> {
    if (!unit.startsWith("day")) return listOf(Measure(unit, value.toDouble(), false))
    val tail = unit.substringAfter(' ', "").let { if (it.isEmpty()) "" else " $it" }
    val list = mutableListOf(
        Measure(unit, value.toDouble(), false),
        Measure("weeks$tail", value / 7.0, value % 7 != 0L),
        Measure("hours$tail", value * 24.0, false),
    )
    if (tail == " left") list.add(Measure("sleeps left", value.toDouble(), false))
    return list
}

private fun formatMeasure(v: Float, decimals: Boolean): String =
    if (decimals) String.format(java.util.Locale.getDefault(), "%,.1f", v) else String.format(java.util.Locale.getDefault(), "%,d", v.roundToLong())

@OptIn(ExperimentalLayoutApi::class)
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
    val since = item.kind == "since"
    val target = if (since) item.targetDate else CountdownMath.nextOccurrence(item.dates, today)
    val isToday = !since && target == today
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isToday, item.targetTime) {
        if (!isToday || item.targetTime == null) return@LaunchedEffect
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            now = System.currentTimeMillis()
        }
    }
    val display = CountdownMath.display(item.dates, today)
    val cells: List<Pair<Long, String>> = when {
        isToday && item.targetTime != null -> {
            val (h, m) = parseTime(item.targetTime)
            val left = maxOf(0L, Dates.toMillis(today, h, m) - now)
            val minutes = ceil(left / 60_000.0).toLong()
            listOf(minutes / 60 to "hours", minutes % 60 to "minutes")
        }
        since -> if (item.targetDate <= today) breakdown(item.targetDate, today) else breakdown(today, item.targetDate)
        CountdownMath.isPast(item.dates, today) -> breakdown(target, today)
        else -> breakdown(today, target)
    }
    val spelled = cells.filter { it.first > 0 }.ifEmpty { cells.takeLast(1) }.joinToString(" · ") { "${it.first} ${it.second}" }
    val options = if (display.numeric) measures(display.value.toLong(), display.unit) else emptyList()
    var unitIndex by remember(id) { mutableIntStateOf(0) }
    val measure = options.getOrNull(unitIndex % maxOf(1, options.size))
    val counted = remember(id) { Animatable(0f) }
    LaunchedEffect(measure?.label, measure?.value) {
        if (measure != null) counted.animateTo(measure.value.toFloat(), tween(700, easing = FastOutSlowInEasing))
    }
    val progress = waitProgress(item, today)
    val fill = remember(id) { Animatable(0f) }
    LaunchedEffect(progress) { fill.animateTo(progress ?: 0f, tween(1000, delayMillis = 200, easing = FastOutSlowInEasing)) }
    val edit = { Nav.openSheet(SheetRoute.CountdownEditor(id)) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("", back = true) {
            HeaderButton(Ic.Pencil, "Edit countdown", 20.dp, edit)
            HeaderButton(Ic.Trash, "Delete countdown", 20.dp) { confirmDelete(item, leave = true) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().background(c.accent).padding(horizontal = GUTTER, vertical = Space.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(
                        (if (since) "Counting up" else whenLabel(target, today)).uppercase(),
                        Type.label, c.onAccent.copy(alpha = 0.8f), Modifier.weight(1f),
                    )
                    if (item.emoji.isNotBlank()) Txt(item.emoji, EMOJI_STYLE.copy(fontSize = 32.sp, lineHeight = 40.sp), unscaled = true)
                }
                Txt(
                    CountdownMath.titleWithAge(item.dates, item.title, today).uppercase(),
                    Type.display.copy(fontSize = 34.sp, lineHeight = 38.sp), c.onAccent,
                    Modifier.padding(top = Space.s).tap(label = "Edit countdown", onClick = edit),
                )
                Txt(dateText(item, today), Type.secondary, c.onAccent.copy(alpha = 0.8f), Modifier.padding(top = Space.xs))

                Column(
                    Modifier.padding(top = Space.xxl).fillMaxWidth()
                        .then(if (options.size > 1) Modifier.tap(label = "Switch unit") { unitIndex = (unitIndex + 1) % options.size } else Modifier),
                ) {
                    if (measure != null) {
                        val text = formatMeasure(counted.value, measure.decimals)
                        val size = when {
                            text.length <= 4 -> 104
                            text.length <= 6 -> 84
                            else -> 64
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.m), itemVerticalAlignment = Alignment.Bottom) {
                            Txt(text, Type.display.copy(fontSize = size.sp, lineHeight = (size + 4).sp), c.onAccent, maxLines = 1, tabularNums = true, unscaled = true)
                            Txt(measure.label.uppercase(), Type.nav, c.onAccent, Modifier.padding(bottom = Space.m), unscaled = true)
                        }
                        if (options.size > 1) {
                            Row(Modifier.padding(top = Space.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                options.indices.forEach { i ->
                                    val on = i == unitIndex % options.size
                                    Box(Modifier.size(if (on) 18.dp else 6.dp, 6.dp).background(c.onAccent.copy(alpha = if (on) 1f else 0.35f)))
                                }
                                Txt("TAP TO SWITCH", Type.label.copy(fontSize = 10.sp), c.onAccent.copy(alpha = 0.7f), Modifier.padding(start = Space.xs), unscaled = true)
                            }
                        }
                    } else {
                        Txt(display.value.removeSuffix(" 🎉").uppercase(), Type.display.copy(fontSize = 72.sp, lineHeight = 76.sp), c.onAccent, unscaled = true)
                    }
                }
                Txt(spelled, Type.secondaryStrong, c.onAccent.copy(alpha = 0.9f), Modifier.padding(top = Space.l))
                if (progress != null) {
                    val grid = CountdownMath.dotGrid(item.dates, Dates.dateOfMillis(item.createdAt), today)
                    Row(Modifier.padding(top = Space.xl).fillMaxWidth()) {
                        Txt("WAITED ${grid.elapsedDays} ${if (grid.elapsedDays == 1) "DAY" else "DAYS"}", Type.label.copy(fontSize = 10.sp), c.onAccent.copy(alpha = 0.8f), Modifier.weight(1f), unscaled = true)
                        Txt("${(progress * 100).toInt()}%", Type.label.copy(fontSize = 10.sp), c.onAccent.copy(alpha = 0.8f), unscaled = true)
                    }
                    Bar(fill.value, c.onAccent, Modifier.padding(top = Space.s))
                } else if (since && item.targetDate <= today) {
                    val day = display.value.toLongOrNull() ?: 0L
                    val step = if (day < 1000) 100L else 1000L
                    val milestone = (day / step + 1) * step
                    val part = (day % step).toFloat() / step
                    Row(Modifier.padding(top = Space.xl).fillMaxWidth()) {
                        Txt("NEXT MILESTONE", Type.label.copy(fontSize = 10.sp), c.onAccent.copy(alpha = 0.8f), Modifier.weight(1f), unscaled = true)
                        Txt("DAY $milestone", Type.label.copy(fontSize = 10.sp), c.onAccent.copy(alpha = 0.8f), unscaled = true)
                    }
                    Bar(part, c.onAccent, Modifier.padding(top = Space.s))
                }
            }

            InfoRow("Repeats", onClick = edit) {
                Txt(
                    when (item.kind) {
                        "yearly" -> "Every year"
                        "monthly" -> "Every month"
                        "since" -> "Counting up"
                        else -> "Once"
                    },
                    Type.secondary, c.ink, maxLines = 1,
                )
            }
            if (!since) {
                InfoRow("Alerts", onClick = edit) {
                    val chosen = CountdownMath.alerts.filter { it.first in item.notify }
                    if (chosen.isEmpty()) Txt("Off", Type.secondary, c.inkMuted)
                    else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        chosen.forEach { (_, label) ->
                            Box(Modifier.border(1.dp, c.hairline).padding(horizontal = Space.s, vertical = 4.dp)) {
                                Txt(label, Type.caption, c.ink, maxLines = 1)
                            }
                        }
                    }
                }
            }
            InfoRow("On the widget", onClick = { CountdownRepo.update(id, "show_in_widget" to !item.showInWidget) }) {
                Toggle(item.showInWidget)
            }
            Spacer(Modifier.height(dockInset()))
        }
    }
}

@Composable
private fun InfoRow(label: String, onClick: () -> Unit, value: @Composable () -> Unit) {
    val c = colors
    Row(
        Modifier.fillMaxWidth().tap(pressedColor = c.hairline, label = label, onClick = onClick).hairlineBottom(c.hairline)
            .heightIn(min = 60.dp).padding(horizontal = GUTTER, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        Txt(label.uppercase(), Type.label, c.inkMuted, maxLines = 1)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { value() }
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
