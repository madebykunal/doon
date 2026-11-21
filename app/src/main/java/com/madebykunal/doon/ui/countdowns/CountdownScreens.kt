package com.madebykunal.doon.ui.countdowns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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

val EMOJI_STYLE = Type.title.copy(fontSize = 40.sp, lineHeight = 52.sp)

@Composable
fun CountdownsScreen() {
    val c = colors
    val today = Ui.today
    val version = Live.version(T.COUNTDOWNS)
    val data = remember(version) { CountdownRepo.all().filter { it.archivedAt == null } }
    var pastOpen by remember { mutableStateOf(false) }
    val upcoming = CountdownMath.sort(data.filter { !CountdownMath.isPast(it.dates, today) }, today) { it.dates }
    val past = if (Prefs.soonShowPast) data.filter { CountdownMath.isPast(it.dates, today) }.sortedByDescending { it.targetDate } else emptyList()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Soon") { SettingsButton() }
        if (upcoming.isEmpty() && past.isEmpty()) {
            EmptyState("Nothing to look forward to yet?", "Add a birthday, a trip, or the day you started something.")
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = dockInset())) {
            items(upcoming, key = { it.id }) { CountdownRow(it, today, Modifier.animateItem()) }
            if (past.isNotEmpty()) {
                item("past") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = Space.l).heightIn(min = 48.dp).tap(label = "Past, ${past.size}") { pastOpen = !pastOpen }.padding(horizontal = GUTTER),
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
private fun CountdownRow(item: Countdown, today: String, modifier: Modifier) {
    val c = colors
    val display = CountdownMath.display(item.dates, today)
    val past = CountdownMath.isPast(item.dates, today)
    val date = CountdownMath.dateLine(item.dates, today) + if (item.targetTime != null && item.kind == "until") " · ${item.targetTime}" else ""
    Row(
        modifier.fillMaxWidth().tap(pressedColor = c.hairline, label = item.title) { Nav.push(Route.CountdownDetail(item.id)) }
            .hairlineBottom(c.hairline).heightIn(min = 76.dp).padding(horizontal = GUTTER, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Txt(CountdownMath.titleWithAge(item.dates, item.title, today), Type.bodyStrong.copy(fontSize = 18.sp), if (past) c.inkMuted else c.ink, maxLines = 1)
            Txt(date, Type.caption, c.inkMuted, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            val value = display.value.removeSuffix(" 🎉")
            Txt(value.uppercase(), Type.display.copy(fontSize = if (display.numeric) 30.sp else 22.sp, lineHeight = 32.sp), if (past) c.inkMuted else c.accentText, maxLines = 1, tabularNums = true, unscaled = true)
            if (display.unit.isNotEmpty()) Txt(display.unit.uppercase(), Type.label.copy(fontSize = 10.sp), c.inkMuted, unscaled = true)
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
    if (years > 0) cells.add(years to if (years == 1L) "Year" else "Years")
    if (years > 0 || months > 0) cells.add(months to if (months == 1L) "Month" else "Months")
    if (years == 0L) cells.add(weeks to if (weeks == 1L) "Week" else "Weeks")
    cells.add(days to if (days == 1L) "Day" else "Days")
    return cells.take(3)
}

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
            listOf(minutes / 60 to "Hours", minutes % 60 to "Minutes")
        }
        since -> if (item.targetDate <= today) breakdown(item.targetDate, today) else breakdown(today, item.targetDate)
        CountdownMath.isPast(item.dates, today) -> breakdown(target, today)
        else -> breakdown(today, target)
    }

    val remove = {
        Dialogs.show(
            "Delete this countdown?",
            item.title,
            listOf(
                DialogAction("Delete", "destructive") {
                    CountdownRepo.softDelete(id)
                    Reminders.syncCountdown(id)
                    Nav.popScreen()
                    Snack.show("Countdown deleted") {
                        CountdownRepo.restore(id)
                        Reminders.syncCountdown(id)
                    }
                },
                DialogAction("Cancel", "cancel"),
            ),
        )
    }
    val kind = when (item.kind) {
        "yearly" -> "Every year"
        "monthly" -> "Every month"
        "since" -> "Counting up"
        else -> "Once"
    }
    val alerts = CountdownMath.alerts.filter { it.first in item.notify }.joinToString(", ") { it.second }.ifEmpty { "Off" }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("", back = true) {
            HeaderButton(Ic.Trash, "Delete countdown", 20.dp, remove)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(horizontal = GUTTER)) {
                Txt(
                    CountdownMath.titleWithAge(item.dates, item.title, today).uppercase(),
                    Type.display.copy(fontSize = 30.sp, lineHeight = 34.sp),
                    modifier = Modifier.tap(label = "Edit countdown") { Nav.openSheet(SheetRoute.CountdownEditor(id)) },
                )
                Txt(
                    CountdownMath.dateLine(item.dates, today) + if (item.targetTime != null && item.kind == "until") " · ${item.targetTime}" else "",
                    Type.secondary, c.inkMuted, Modifier.padding(top = Space.xs),
                )
            }

            Column(Modifier.fillMaxWidth().padding(top = Space.xxl).background(c.accent).padding(horizontal = GUTTER, vertical = Space.xl)) {
                Txt(
                    (if (since) "Since ${Dates.format(item.targetDate, "d MMM yyyy")}" else whenLabel(target, today)).uppercase(),
                    Type.label, c.onAccent.copy(alpha = 0.8f),
                )
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.m), modifier = Modifier.padding(top = Space.s)) {
                    if (display.numeric) Odometer(display.value, Type.display.copy(fontSize = 96.sp, lineHeight = 100.sp), c.onAccent)
                    else Txt(display.value.removeSuffix(" 🎉").uppercase(), Type.display.copy(fontSize = 64.sp, lineHeight = 70.sp), c.onAccent, unscaled = true)
                    if (display.unit.isNotEmpty()) {
                        Txt(display.unit.uppercase(), Type.nav, c.onAccent, Modifier.padding(bottom = Space.m), unscaled = true)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = Space.l).height(IntrinsicSize.Min)) {
                    cells.forEachIndexed { i, (value, label) ->
                        if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(c.onAccent.copy(alpha = 0.25f)))
                        Column(Modifier.weight(1f).padding(start = if (i > 0) Space.l else 0.dp)) {
                            Txt(value.toString(), Type.display.copy(fontSize = 34.sp, lineHeight = 38.sp), c.onAccent, tabularNums = true, unscaled = true)
                            Txt(label.uppercase(), Type.label.copy(fontSize = 10.sp), c.onAccent.copy(alpha = 0.8f), unscaled = true)
                        }
                    }
                }
            }

            InfoRow("Repeats", kind)
            if (!since) InfoRow("Alerts", alerts)
            InfoRow("On the widget", if (item.showInWidget) "Yes" else "No")
            Txt("Tap the title to edit.", Type.caption, c.inkMuted, Modifier.padding(horizontal = GUTTER, vertical = Space.l))
            Spacer(Modifier.height(dockInset()))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val c = colors
    Row(
        Modifier.fillMaxWidth().hairlineBottom(c.hairline).heightIn(min = 56.dp).padding(horizontal = GUTTER),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(label.uppercase(), Type.label, c.inkMuted, Modifier.weight(1f))
        Txt(value, Type.secondary, c.ink, maxLines = 1)
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
