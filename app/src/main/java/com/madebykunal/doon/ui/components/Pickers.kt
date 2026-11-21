package com.madebykunal.doon.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.pad2
import com.madebykunal.doon.lib.parseTime
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.state.Ui
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.HIT_TARGET
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import kotlin.math.abs

private const val YEARS_PER_PAGE = 12
private val CELL_HEIGHT = 44.dp
private val WEEKDAY_HEIGHT = 24.dp

@Composable
fun DatePicker(
    value: DateStr?,
    onChange: (DateStr) -> Unit,
    openDates: Set<String> = emptySet(),
    onMonthShown: ((DateStr) -> Unit)? = null,
    showToday: Boolean = false,
) {
    val c = colors
    val today = Ui.today
    val start = value ?: today
    var shown by remember { mutableStateOf(Dates.makeDate(Dates.year(start), Dates.month(start), 1)) }
    var direction by remember { mutableIntStateOf(0) }
    var choosingYear by remember { mutableStateOf(false) }
    var yearPage by remember { mutableIntStateOf(Dates.year(shown) / YEARS_PER_PAGE) }
    val threshold = with(LocalDensity.current) { 32.dp.toPx() }

    fun showMonth(first: DateStr, dir: Int) {
        direction = dir
        shown = first
        onMonthShown?.invoke(first)
    }

    fun shift(by: Int) = showMonth(Dates.addMonths(shown, by), Integer.signum(by))

    Column(Modifier.padding(horizontal = GUTTER - Space.s), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Row(Modifier.heightIn(min = HIT_TARGET), verticalAlignment = Alignment.CenterVertically) {
            val pageStart = yearPage * YEARS_PER_PAGE
            Row(
                Modifier.tap(label = "Choose a year") {
                    yearPage = Dates.year(shown) / YEARS_PER_PAGE
                    choosingYear = !choosingYear
                }.padding(horizontal = Space.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                Txt(if (choosingYear) "$pageStart – ${pageStart + YEARS_PER_PAGE - 1}" else Dates.format(shown, "MMMM yyyy"), Type.h2)
                Icon(Ic.ChevronDown, c.inkMuted, 16.dp)
            }
            Spacer(Modifier.weight(1f))
            if (showToday && value != today) {
                Txt("Today", Type.secondaryStrong, c.accentText, Modifier.padding(end = Space.xs).tap(label = "Jump to today") { onChange(today) }.padding(horizontal = Space.s))
            }
            NavButton(Ic.ChevronLeft, if (choosingYear) "Earlier years" else "Previous month") { if (choosingYear) yearPage-- else shift(-1) }
            NavButton(Ic.ChevronRight, if (choosingYear) "Later years" else "Next month") { if (choosingYear) yearPage++ else shift(1) }
        }

        if (choosingYear) {
            val pageStart = yearPage * YEARS_PER_PAGE
            Column(Modifier.height(WEEKDAY_HEIGHT + CELL_HEIGHT * 5)) {
                for (r in 0 until 3) {
                    Row(Modifier.weight(1f)) {
                        for (col in 0 until 4) {
                            val year = pageStart + r * 4 + col
                            val selected = year == Dates.year(shown)
                            Box(
                                Modifier.weight(1f).fillMaxWidth().tap {
                                    choosingYear = false
                                    showMonth(Dates.makeDate(year, Dates.month(shown), 1), 0)
                                },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.clip(CircleShape).background(if (selected) c.accent else androidx.compose.ui.graphics.Color.Transparent)
                                        .padding(horizontal = Space.m, vertical = Space.s),
                                ) {
                                    Txt(year.toString(), Type.secondaryStrong, if (selected) c.onAccent else c.ink, tabularNums = true)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            var drag by remember { mutableStateOf(0f) }
            AnimatedContent(
                targetState = shown,
                transitionSpec = {
                    (slideInHorizontally(tween(130)) { (it * 0.08f * direction).toInt() } + fadeIn(tween(130), 0.3f)) togetherWith fadeOut(tween(60))
                },
                modifier = Modifier.pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0f },
                        onDragEnd = { if (abs(drag) > threshold) shift(if (drag < 0) 1 else -1) },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            drag += amount
                        },
                    )
                },
                label = "month",
            ) { first ->
                MonthGrid(Dates.year(first), Dates.month(first), value, today, Prefs.weekStart, openDates, onChange)
            }
        }
    }
}

@Composable
private fun NavButton(ic: Ic, label: String, onClick: () -> Unit) {
    Box(Modifier.size(HIT_TARGET - 4.dp, HIT_TARGET).tap(pressedAlpha = 0.5f, label = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(ic, colors.inkMuted, 20.dp)
    }
}

@Composable
fun MonthGrid(year: Int, month: Int, selected: DateStr?, today: DateStr, weekStartsOn: Int, openDates: Set<String>, onPick: (DateStr) -> Unit) {
    val c = colors
    val cells = Dates.monthGrid(year, month, weekStartsOn)
    val rows = (0 until 6).map { cells.subList(it * 7, it * 7 + 7) }.filter { row -> row.any { Dates.month(it) == month } }
    Column {
        Row {
            Dates.weekDays(cells[0]).forEach {
                Box(Modifier.weight(1f).height(WEEKDAY_HEIGHT), contentAlignment = Alignment.Center) {
                    Txt(Dates.format(it, "EEEEE"), Type.micro, c.inkMuted)
                }
            }
        }
        rows.forEach { row ->
            Row {
                row.forEach { date ->
                    if (Dates.month(date) != month) {
                        Spacer(Modifier.weight(1f).height(CELL_HEIGHT))
                    } else {
                        val isSelected = date == selected
                        val isToday = date == today
                        Column(
                            Modifier.weight(1f).height(CELL_HEIGHT).tap { onPick(date) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(
                                    if (isSelected) c.accent else if (isToday) c.accentTint else androidx.compose.ui.graphics.Color.Transparent,
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Txt(
                                    Dates.day(date).toString(),
                                    if (isSelected || isToday) Type.secondaryStrong else Type.secondary,
                                    if (isSelected) c.onAccent else if (isToday) c.accentText else c.ink,
                                    tabularNums = true,
                                )
                            }
                            Box(Modifier.padding(top = 2.dp).size(4.dp).alpha(if (date in openDates) 0.7f else 0f).clip(CircleShape).background(c.inkMuted))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimePicker(value: String, onChange: (String) -> Unit) {
    val (hour, minute) = parseTime(value)
    fun set(h: Int, m: Int) = onChange("${pad2((h + 24) % 24)}:${pad2((m + 60) % 60)}")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Stepper("Hour", pad2(hour), { set(hour - 1, minute) }, { set(hour + 1, minute) })
        Txt(":", Type.h2)
        Stepper("Minute", pad2(minute), { set(hour, minute - 5) }, { set(hour, minute + 5) })
    }
}

@Composable
private fun Stepper(label: String, value: String, onDown: () -> Unit, onUp: () -> Unit) {
    val c = colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(HIT_TARGET - 4.dp, HIT_TARGET).tap(label = "$label down", onClick = onDown), contentAlignment = Alignment.Center) {
            Icon(Ic.ChevronLeft, c.ink, 18.dp)
        }
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) { Txt(value, Type.h2, tabularNums = true) }
        Box(Modifier.size(HIT_TARGET - 4.dp, HIT_TARGET).tap(label = "$label up", onClick = onUp), contentAlignment = Alignment.Center) {
            Icon(Ic.ChevronRight, c.ink, 18.dp)
        }
    }
}
