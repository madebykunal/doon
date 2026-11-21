package com.madebykunal.doon.ui.habits

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.madebykunal.doon.data.Habit
import com.madebykunal.doon.data.HabitRepo
import com.madebykunal.doon.data.Live
import com.madebykunal.doon.data.T
import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.DayState
import com.madebykunal.doon.lib.HabitMath
import com.madebykunal.doon.state.DialogAction
import com.madebykunal.doon.state.Dialogs
import com.madebykunal.doon.state.Nav
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.state.Route
import com.madebykunal.doon.state.SheetRoute
import com.madebykunal.doon.state.Snack
import com.madebykunal.doon.state.Ui
import com.madebykunal.doon.ui.components.EmojiTile
import com.madebykunal.doon.ui.components.EmptyState
import com.madebykunal.doon.ui.components.HeaderButton
import com.madebykunal.doon.ui.components.Ic
import com.madebykunal.doon.ui.components.Icon
import com.madebykunal.doon.ui.components.ScreenHeader
import com.madebykunal.doon.ui.components.SectionLabel
import com.madebykunal.doon.ui.components.SettingsButton
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.dockInset
import com.madebykunal.doon.ui.components.tap
import androidx.compose.ui.unit.sp
import com.madebykunal.doon.ui.components.hairlineBottom
import com.madebykunal.doon.ui.countdowns.CountRow
import com.madebykunal.doon.ui.countdowns.EMOJI_STYLE
import com.madebykunal.doon.ui.theme.DoonColors
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.HIT_TARGET
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import java.text.NumberFormat

@Composable
fun HabitsScreen() {
    val version = Live.version(T.HABITS)
    val habits = remember(version) { HabitRepo.all().filter { it.archivedAt == null } }
    val building = habits.filter { it.kind == "build" }
    val quitting = habits.filter { it.kind == "quit" }
    val both = building.isNotEmpty() && quitting.isNotEmpty()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Habits") { SettingsButton() }
        if (habits.isEmpty()) {
            EmptyState("What do you want to change?", "Add something to do more often, or something to stop.")
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = dockInset())) {
            if (both) item("Building") { Txt("BUILDING", Type.label, colors.inkMuted, Modifier.padding(start = GUTTER, top = Space.xl, bottom = Space.s)) }
            items(building, key = { it.id }) { HabitRow(it) }
            if (both) item("Quitting") { Txt("QUITTING", Type.label, colors.inkMuted, Modifier.padding(start = GUTTER, top = Space.xl, bottom = Space.s)) }
            items(quitting, key = { it.id }) { HabitRow(it) }
        }
    }
}

@Composable
private fun dotStyle(state: DayState, c: DoonColors): Modifier = when (state) {
    DayState.Done, DayState.Clean -> Modifier.background(c.accent, RectangleShape)
    DayState.Partial -> Modifier.background(c.accent.copy(alpha = 0.45f), RectangleShape)
    DayState.Missed -> Modifier.background(c.inkMuted.copy(alpha = 0.3f), RectangleShape)
    DayState.Slip -> Modifier.border(1.5.dp, c.inkMuted, RectangleShape)
    DayState.Open -> Modifier.border(1.5.dp, c.accent, RectangleShape)
    else -> Modifier.background(c.hairline, RectangleShape)
}

@Composable
private fun HabitRow(habit: Habit) {
    val c = colors
    val today = Ui.today
    val logsV = Live.version(T.HABIT_LOGS)
    val logs = remember(logsV, habit.id) { HabitRepo.logs(habit.id) }
    val s = habit.schedule
    val streak = HabitMath.currentStreak(s, logs, today)
    val count = HabitMath.countOn(logs, today)
    val dayOff = habit.kind == "build" && !HabitMath.isScheduled(habit.days, today) && count < habit.target
    val week = (0 until 7).map { HabitMath.dayState(s, logs, Dates.addDays(today, it - 6), today) }
    Row(
        Modifier.fillMaxWidth().tap(pressedColor = c.hairline, label = habit.title) { Nav.push(Route.HabitDetail(habit.id)) }.hairlineBottom(c.hairline).padding(horizontal = GUTTER, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Txt(habit.emoji, Type.h2, unscaled = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Txt(habit.title.uppercase(), Type.listName.copy(fontSize = 20.sp, lineHeight = 24.sp), maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { week.forEach { Box(Modifier.size(7.dp).then(dotStyle(it, c))) } }
                Txt(
                    if (habit.kind == "quit") "Last 7 days" else if (dayOff) "Not due today" else "$streak ${HabitMath.streakUnit(s, streak)}",
                    Type.caption, c.inkMuted, maxLines = 1,
                )
            }
        }
        if (habit.kind == "build") {
            TickRing(count, habit.target, dayOff) { HabitRepo.setCount(habit.id, today, HabitMath.nextCount(s, count)) }
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Txt(streak.toString(), Type.display, c.accentText, maxLines = 1, tabularNums = true)
                Txt(HabitMath.streakUnit(s, streak).uppercase(), Type.label.copy(fontSize = 10.sp), c.accentText)
            }
        }
    }
}

@Composable
private fun TickRing(count: Int, target: Int, dimmed: Boolean, onClick: () -> Unit) {
    val c = colors
    val done = count >= target
    val progress by animateFloatAsState(minOf(1f, count / target.toFloat()), tween(160), label = "ring")
    Box(Modifier.size(HIT_TARGET).alpha(if (dimmed) 0.45f else 1f).tap(label = if (target == 1) "Done" else "Add one", onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(30.dp)) {
            val stroke = 2.5.dp.toPx()
            val r = (size.width - stroke) / 2
            if (done) drawCircle(c.accent, r)
            drawCircle(c.hairline, r, style = Stroke(stroke))
            drawArc(c.accent, -90f, 360f * progress, false, Offset(stroke / 2, stroke / 2), Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        if (done) Icon(Ic.Check, c.onAccent, 16.dp, stroke = 3f)
        else if (target > 1) Txt("$count/$target", Type.micro, c.inkMuted, tabularNums = true, unscaled = true)
    }
}

private fun plural(n: Int, word: String) = "${NumberFormat.getIntegerInstance().format(n)} $word${if (n == 1) "" else "s"}"

@Composable
fun HabitDetailScreen(id: String) {
    val c = colors
    val today = Ui.today
    val habitV = Live.version(T.HABITS)
    val logsV = Live.version(T.HABIT_LOGS)
    val habit = remember(habitV, id) { HabitRepo.get(id) }
    val logs = remember(logsV, id) { HabitRepo.logs(id) }
    if (habit == null || habit.deletedAt != null) {
        ScreenHeader("", back = true)
        return
    }
    val s = habit.schedule
    val quit = habit.kind == "quit"
    val streak = HabitMath.currentStreak(s, logs, today)
    val best = HabitMath.bestStreak(s, logs, today)
    val count = HabitMath.countOn(logs, today)
    val done = count >= habit.target
    val inRange = logs.filter { it.date >= habit.startDate && it.date <= today }
    val total = if (quit) inRange.size else inRange.count { it.count >= habit.target }

    val remove = {
        Dialogs.show(
            "Delete this habit?",
            "${habit.title} and its history",
            listOf(
                DialogAction("Cancel", "cancel"),
                DialogAction("Delete", "destructive") {
                    HabitRepo.softDelete(id)
                    Nav.popScreen()
                    Snack.show("Habit deleted") { HabitRepo.restore(id) }
                },
            ),
        )
    }
    val logToday = {
        val previous = count
        val next = HabitMath.nextCount(s, count)
        HabitRepo.setCount(id, today, next)
        if (quit && next > 0) Snack.show("Slip logged") { HabitRepo.setCount(id, today, previous) }
    }
    val label = when {
        quit -> if (count > 0) "Undo today’s slip" else "I slipped today"
        done -> "Done today · undo"
        !HabitMath.isScheduled(habit.days, today) -> "Log an extra day"
        habit.target > 1 -> "Log one · $count of ${habit.target}"
        else -> "Mark today done"
    }
    val filled = if (quit) count == 0 else done

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("", back = true) {
            HeaderButton(Ic.Pencil, "Edit habit", 18.dp) { Nav.openSheet(SheetRoute.HabitEditor(id)) }
            HeaderButton(Ic.Trash, "Delete habit", 18.dp, remove)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = GUTTER), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Txt(habit.emoji, EMOJI_STYLE, unscaled = true)
            Txt(habit.title.uppercase(), Type.display)
            Txt(HabitMath.scheduleLine(s, Prefs.weekStart), Type.secondary, c.inkMuted)
            Spacer(Modifier.height(Space.xl))
            CountRow(streak.toString(), HabitMath.streakUnit(s, streak), true)
            val buttonMod = when {
                filled && !quit -> Modifier.background(c.accentTint, RectangleShape)
                quit -> Modifier.border(1.dp, c.hairline, RectangleShape)
                else -> Modifier.background(c.accent, RectangleShape)
            }
            Box(
                Modifier.padding(top = Space.m, bottom = Space.xl).heightIn(min = HIT_TARGET).clip(RectangleShape).then(buttonMod)
                    .tap(pressedAlpha = 0.8f, onClick = logToday).padding(horizontal = Space.xl),
                contentAlignment = Alignment.Center,
            ) {
                Txt(label, Type.secondaryStrong, if (quit) c.ink else if (filled) c.accentText else c.onAccent)
            }
            BoxWithConstraints {
                HabitGrid(maxWidth, today, Prefs.weekStart, { HabitMath.dayState(s, logs, it, today) }) { date ->
                    if (date >= habit.startDate) HabitRepo.setCount(id, date, HabitMath.nextCount(s, HabitMath.countOn(logs, date)))
                }
            }
            Txt(if (quit) "Best run ${plural(best, "day")} · ${plural(total, "slip")}" else "Best streak ${plural(best, "day")} · Done ${plural(total, "day")}", Type.caption, c.inkMuted)
            Txt(if (quit) "Tap a day to mark or clear a slip." else "Tap a day to fill in one you missed.", Type.caption, c.inkMuted)
            Spacer(Modifier.height(dockInset()))
        }
    }
}

@Composable
private fun HabitGrid(width: Dp, today: DateStr, weekStartsOn: Int, stateOf: (DateStr) -> DayState, onPressDay: (DateStr) -> Unit) {
    val c = colors
    val label = 16.dp
    val gridWidth = width - label
    val weeks = minOf(26, maxOf(1, (gridWidth / 20.dp).toInt()))
    val pitch = gridWidth / weeks
    val height = pitch * 7
    val first = Dates.addDays(Dates.startOfWeek(today, weekStartsOn), -(weeks - 1) * 7)
    val states = (0 until weeks * 7).map { stateOf(Dates.addDays(first, it)) }
    Row(Modifier.padding(vertical = Space.s)) {
        Box(Modifier.width(label).height(height)) {
            listOf(0, 3, 6).forEach { row ->
                Txt(Dates.format(Dates.addDays(first, row), "EEEEE"), Type.micro, c.inkMuted, Modifier.offset(y = pitch * row + pitch / 2 - 7.dp), unscaled = true)
            }
        }
        Canvas(
            Modifier.width(gridWidth).height(height).pointerInput(first, weeks) {
                detectTapGestures { pos ->
                    val p = size.width / weeks.toFloat()
                    val week = (pos.x / p).toInt()
                    val row = (pos.y / p).toInt()
                    if (week !in 0 until weeks || row !in 0..6) return@detectTapGestures
                    val date = Dates.addDays(first, week * 7 + row)
                    if (date <= today) onPressDay(date)
                }
            },
        ) {
            val p = size.width / weeks
            val r = minOf(p, 20.dp.toPx()) * 0.34f
            val ring = Stroke(1.5.dp.toPx())
            states.forEachIndexed { i, state ->
                val center = Offset((i / 7) * p + p / 2, (i % 7) * p + p / 2)
                when (state) {
                    DayState.Done, DayState.Clean -> drawCircle(c.accent, r, center)
                    DayState.Partial -> drawCircle(c.accent.copy(alpha = 0.45f), r, center)
                    DayState.Missed -> drawCircle(c.inkMuted.copy(alpha = 0.28f), r, center)
                    DayState.Slip -> drawCircle(c.inkMuted, r, center, style = ring)
                    DayState.Open -> drawCircle(c.accent, r, center, style = ring)
                    DayState.Off -> drawCircle(c.inkMuted.copy(alpha = 0.1f), r, center)
                    DayState.None -> drawCircle(c.hairline, r, center)
                }
            }
        }
    }
}
