package com.madebykunal.doon.ui.habits

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madebykunal.doon.data.Habit
import com.madebykunal.doon.data.HabitRepo
import com.madebykunal.doon.data.Live
import com.madebykunal.doon.data.T
import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.DayLog
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
import com.madebykunal.doon.ui.components.EmptyState
import com.madebykunal.doon.ui.components.HeaderButton
import com.madebykunal.doon.ui.components.Ic
import com.madebykunal.doon.ui.components.ScreenHeader
import com.madebykunal.doon.ui.components.SettingsButton
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.dockInset
import com.madebykunal.doon.ui.components.tap
import com.madebykunal.doon.ui.theme.DoonColors
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors

private enum class Slot { Due, Done, Rest, Stop }

private class HabitDay(val habit: Habit, val logs: List<DayLog>, val count: Int, val slot: Slot)

private fun dayOf(habit: Habit, logs: List<DayLog>, today: DateStr): HabitDay {
    val count = HabitMath.countOn(logs, today)
    val slot = when {
        habit.kind == "quit" -> Slot.Stop
        count >= habit.target -> Slot.Done
        HabitMath.isScheduled(habit.days, today) -> Slot.Due
        else -> Slot.Rest
    }
    return HabitDay(habit, logs, count, slot)
}

private fun logOnce(habit: Habit, count: Int, today: DateStr) {
    val next = HabitMath.nextCount(habit.schedule, count)
    HabitRepo.setCount(habit.id, today, next)
    if (habit.kind == "quit" && next > 0) Snack.show("It’s okay. Start again today.") { HabitRepo.setCount(habit.id, today, count) }
}

@Composable
fun HabitsScreen() {
    val c = colors
    val today = Ui.today
    val version = Live.version(T.HABITS)
    val logsV = Live.version(T.HABIT_LOGS)
    val habits = remember(version) { HabitRepo.all().filter { it.archivedAt == null } }
    val days = remember(version, logsV, today) { habits.map { dayOf(it, HabitRepo.logs(it.id), today) } }
    val groups = days.groupBy { it.slot }
    val dueCount = (groups[Slot.Due]?.size ?: 0) + (groups[Slot.Done]?.count { HabitMath.isScheduled(it.habit.days, today) } ?: 0)
    val doneCount = groups[Slot.Done]?.count { HabitMath.isScheduled(it.habit.days, today) } ?: 0
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Habits") { SettingsButton() }
        if (habits.isEmpty()) {
            EmptyState("Start with something tiny", "Pick one small thing to do more, or one thing to do less. Tap + to begin.")
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = dockInset())) {
            if (dueCount > 0) item("today") { TodayProgress(doneCount, dueCount, Modifier.animateItem()) }
            listOf(Slot.Due to "To do today", Slot.Done to "Done today", Slot.Rest to "Not today", Slot.Stop to "Stopping").forEach { (slot, title) ->
                val rows = groups[slot] ?: return@forEach
                item("h:$slot") {
                    Txt(title.uppercase(), Type.label, c.inkMuted, Modifier.animateItem().padding(horizontal = GUTTER).padding(top = Space.xl, bottom = Space.xs))
                }
                items(rows, key = { it.habit.id }) { HabitRow(it, today, Modifier.animateItem()) }
            }
        }
    }
}

@Composable
private fun TodayProgress(done: Int, total: Int, modifier: Modifier) {
    val c = colors
    Column(modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(top = Space.xs)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Txt("$done", Type.display.copy(fontSize = 40.sp, lineHeight = 42.sp), c.accentText, tabularNums = true, unscaled = true)
            Txt(" OF $total DONE TODAY", Type.nav.copy(fontSize = 18.sp, lineHeight = 22.sp), c.ink, Modifier.padding(bottom = 4.dp), unscaled = true)
        }
        Row(Modifier.fillMaxWidth().padding(top = Space.s), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(total) { i ->
                val fill by animateColorAsState(if (i < done) c.accent else c.hairline, tween(300, delayMillis = 40 * i), label = "seg")
                Box(Modifier.weight(1f).height(6.dp).background(fill))
            }
        }
        if (done == total) Txt("All done. That’s how habits are made.", Type.caption, c.inkMuted, Modifier.padding(top = Space.s))
    }
}

private fun Modifier.weekDay(state: DayState, c: DoonColors): Modifier = when (state) {
    DayState.Done, DayState.Clean -> background(c.accent)
    DayState.Partial -> background(c.accent.copy(alpha = 0.45f))
    DayState.Slip -> border(1.5.dp, c.inkMuted)
    DayState.Open -> border(1.5.dp, c.accent)
    DayState.Missed -> background(c.inkMuted.copy(alpha = 0.3f))
    else -> background(c.hairline)
}

@Composable
private fun HabitRow(day: HabitDay, today: DateStr, modifier: Modifier) {
    val c = colors
    val habit = day.habit
    val s = habit.schedule
    val quit = habit.kind == "quit"
    val streak = HabitMath.currentStreak(s, day.logs, today)
    val best = remember(day.logs, today) { HabitMath.bestStreak(s, day.logs, today) }
    val stats = when {
        quit && best > streak -> "Longest run: $best ${if (best == 1) "day" else "days"}"
        quit -> "Since ${Dates.format(habit.startDate, "d MMM")}"
        streak > 0 -> "$streak in a row · best $best"
        best > 0 -> "Best run: $best ${if (best == 1) "day" else "days"}"
        else -> HabitMath.scheduleLine(s, Prefs.weekStart)
    }
    Column(
        modifier.fillMaxWidth().padding(horizontal = GUTTER, vertical = 5.dp).alpha(if (day.slot == Slot.Rest) 0.6f else 1f)
            .border(1.dp, if (day.slot == Slot.Done) c.accent.copy(alpha = 0.5f) else c.hairline)
            .tap(pressedColor = c.hairline, pressedScale = 0.985f, label = habit.title) { Nav.push(Route.HabitDetail(habit.id)) }
            .padding(Space.l),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Space.l)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt(habit.title, Type.bodyStrong.copy(fontSize = 19.sp, lineHeight = 24.sp), maxLines = 2)
                habit.cue?.takeIf { it.isNotBlank() }?.let { Txt(it, Type.secondary, c.inkMuted, maxLines = 1) }
                Txt(stats, Type.caption, c.inkMuted, maxLines = 1)
            }
            if (quit) {
                Column(horizontalAlignment = Alignment.End) {
                    Txt(streak.toString().padStart(2, '0'), Type.display.copy(fontSize = 34.sp, lineHeight = 36.sp), c.accentText, maxLines = 1, tabularNums = true, unscaled = true)
                    Txt(HabitMath.streakUnit(s, streak).uppercase(), Type.label.copy(fontSize = 10.sp), c.inkMuted, unscaled = true)
                }
            } else {
                LogBlock(day.count, habit.target) { logOnce(habit, day.count, today) }
            }
        }
        if (Prefs.habitWeekDots || quit) {
            Row(Modifier.fillMaxWidth().padding(top = Space.l), verticalAlignment = Alignment.CenterVertically) {
                if (Prefs.habitWeekDots) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        (0 until 7).forEach { i ->
                            val date = Dates.addDays(today, i - 6)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Box(Modifier.size(14.dp).weekDay(HabitMath.dayState(s, day.logs, date, today), c))
                                Txt(Dates.format(date, "EEEEE"), Type.micro.copy(fontSize = 9.sp, lineHeight = 10.sp), if (i == 6) c.ink else c.inkMuted, unscaled = true)
                            }
                        }
                    }
                } else Spacer(Modifier.weight(1f))
                if (quit) {
                    val slipped = day.count > 0
                    Box(
                        Modifier.tap(pressedScale = 0.92f, haptic = true, label = if (slipped) "Undo slip" else "I slipped") { logOnce(habit, day.count, today) }
                            .heightIn(min = 40.dp).border(1.dp, if (slipped) c.inkMuted else c.hairline).padding(horizontal = Space.m),
                        contentAlignment = Alignment.Center,
                    ) {
                        Txt(if (slipped) "UNDO SLIP" else "I SLIPPED", Type.nav.copy(fontSize = 15.sp, lineHeight = 18.sp), if (slipped) c.ink else c.inkMuted, unscaled = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogBlock(count: Int, target: Int, onClick: () -> Unit) {
    val c = colors
    val done = count >= target
    val part by animateFloatAsState(minOf(1f, count / target.toFloat()), tween(220), label = "part")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(done) {
        if (done) {
            pop.snapTo(0.85f)
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }
    Box(
        Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .tap(pressedScale = 0.9f, haptic = true, label = if (done) "Undo" else "Done") { onClick() }
            .size(72.dp, 44.dp).border(1.5.dp, if (count > 0) c.accent else c.inkMuted.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(part).align(Alignment.CenterStart).background(c.accent))
        val text = when {
            done -> "DONE"
            target > 1 -> "$count/$target"
            else -> "DO IT"
        }
        Txt(text, Type.nav.copy(fontSize = 17.sp, lineHeight = 20.sp), if (part >= 0.5f) c.onAccent else c.ink, maxLines = 1, tabularNums = true, unscaled = true)
    }
}

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
    val scheduled = HabitMath.isScheduled(habit.days, today)
    val inRange = logs.filter { it.date >= habit.startDate && it.date <= today }
    val total = if (quit) inRange.size else inRange.count { it.count >= habit.target }
    val rate = if (quit) null else HabitMath.recentRate(s, logs, today)
    val edit = { Nav.openSheet(SheetRoute.HabitEditor(id)) }

    val remove = {
        Dialogs.show(
            "Delete this habit?",
            "${habit.title} and all its days",
            listOf(
                DialogAction("Delete", "destructive") {
                    HabitRepo.softDelete(id)
                    Nav.popScreen()
                    Snack.show("Habit deleted") { HabitRepo.restore(id) }
                },
                DialogAction("Cancel", "cancel"),
            ),
        )
    }
    val nudge = when {
        quit && streak == 0 -> "Day one. You can do this."
        quit -> "$streak ${if (streak == 1) "day" else "days"} without it. Keep going."
        done -> if (streak > 1) "Nice. That’s $streak days in a row." else "Nice. You showed up today."
        !scheduled -> "Not planned for today. Doing it anyway still counts."
        HabitMath.missedLastTime(s, logs, today) -> "Missed last time? That’s fine. Just don’t miss twice."
        else -> "Small is fine. Just start."
    }
    val button = when {
        quit -> if (count > 0) "UNDO SLIP" else "I SLIPPED TODAY"
        done -> "DONE TODAY"
        habit.target > 1 -> "DONE ONCE · $count OF ${habit.target}"
        else -> "I DID IT TODAY"
    }
    val filled = !quit && !done

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("", back = true) {
            HeaderButton(Ic.Pencil, "Edit habit", 20.dp, edit)
            HeaderButton(Ic.Trash, "Delete habit", 20.dp, remove)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().padding(horizontal = GUTTER)) {
                Txt((if (quit) "Stopping" else "Doing").uppercase(), Type.label, c.accentText)
                Txt(habit.title.uppercase(), Type.display.copy(fontSize = 34.sp, lineHeight = 38.sp), modifier = Modifier.padding(top = Space.s).tap(label = "Edit habit", onClick = edit))
                val cue = habit.cue?.takeIf { it.isNotBlank() }
                Txt(listOfNotNull(cue, HabitMath.scheduleLine(s, Prefs.weekStart)).joinToString(" · "), Type.secondary, c.inkMuted, Modifier.padding(top = Space.xs))

                val shown = remember(id) { Animatable(0f) }
                LaunchedEffect(streak) { shown.animateTo(streak.toFloat(), tween(600, easing = FastOutSlowInEasing)) }
                Row(Modifier.padding(top = Space.xxl), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    Txt(shown.value.toInt().toString(), Type.display.copy(fontSize = 96.sp, lineHeight = 98.sp), c.accentText, maxLines = 1, tabularNums = true, unscaled = true)
                    Txt(HabitMath.streakUnit(s, streak).uppercase(), Type.nav, c.ink, Modifier.padding(bottom = Space.m), unscaled = true)
                }
                Txt(nudge, Type.secondary, c.inkMuted, Modifier.padding(top = Space.xs))

                val fill by animateColorAsState(if (filled) c.accent else Color.Transparent, tween(200), label = "btn")
                Box(
                    Modifier.padding(top = Space.l).fillMaxWidth().tap(pressedScale = 0.97f, haptic = true, label = button) { logOnce(habit, count, today) }
                        .heightIn(min = 56.dp).background(fill).border(1.5.dp, if (filled) c.accent else c.hairline),
                    contentAlignment = Alignment.Center,
                ) {
                    Txt(button, Type.nav, if (filled) c.onAccent else if (quit) c.ink else c.accentText, unscaled = true)
                }
            }

            Row(Modifier.padding(horizontal = GUTTER).padding(top = Space.xxl).fillMaxWidth().height(IntrinsicSize.Min).border(1.dp, c.hairline)) {
                val cells = buildList {
                    add(best.toString() to if (quit) "Longest run" else "Best run")
                    add(total.toString() to if (quit) "Slips" else "Days done")
                    if (rate != null) add("$rate%" to "Last 30 days")
                }
                cells.forEachIndexed { i, (value, label) ->
                    if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(c.hairline))
                    Column(Modifier.weight(1f).padding(Space.m), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Txt(value, Type.display.copy(fontSize = 28.sp, lineHeight = 30.sp), maxLines = 1, tabularNums = true, unscaled = true)
                        Txt(label.uppercase(), Type.label.copy(fontSize = 10.sp), c.inkMuted, maxLines = 1, unscaled = true)
                    }
                }
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(top = Space.xxl)) {
                Txt("YOUR DAYS", Type.label, c.inkMuted)
                BoxWithConstraints(Modifier.padding(top = Space.s)) {
                    HabitGrid(maxWidth, today, Prefs.weekStart, { HabitMath.dayState(s, logs, it, today) }) { date ->
                        if (date >= habit.startDate) HabitRepo.setCount(id, date, HabitMath.nextCount(s, HabitMath.countOn(logs, date)))
                    }
                }
                Txt(
                    if (quit) "Tap a day to add or remove a slip." else "Forgot to log a day? Tap it to fill it in.",
                    Type.caption, c.inkMuted, Modifier.padding(top = Space.xs),
                )
            }
            Spacer(Modifier.height(dockInset() + Space.xl))
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
    Row {
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
            val side = p * 0.78f
            val inset = (p - side) / 2
            val ring = Stroke(1.5.dp.toPx())
            val box = Size(side, side)
            states.forEachIndexed { i, state ->
                val at = Offset((i / 7) * p + inset, (i % 7) * p + inset)
                when (state) {
                    DayState.Done, DayState.Clean -> drawRect(c.accent, at, box)
                    DayState.Partial -> drawRect(c.accent.copy(alpha = 0.45f), at, box)
                    DayState.Missed -> drawRect(c.inkMuted.copy(alpha = 0.28f), at, box)
                    DayState.Slip -> drawRect(c.inkMuted, at, box, style = ring)
                    DayState.Open -> drawRect(c.accent, at, box, style = ring)
                    DayState.Off -> drawRect(c.inkMuted.copy(alpha = 0.1f), at, box)
                    DayState.None -> drawRect(c.hairline, at, box)
                }
            }
        }
    }
}
