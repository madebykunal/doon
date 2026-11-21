package com.madebykunal.doon.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.madebykunal.doon.data.CountdownRepo
import com.madebykunal.doon.data.HabitRepo
import com.madebykunal.doon.data.Home
import com.madebykunal.doon.data.ListRepo
import com.madebykunal.doon.data.Live
import com.madebykunal.doon.data.SeriesRepo
import com.madebykunal.doon.data.T
import com.madebykunal.doon.data.TodoRepo
import com.madebykunal.doon.lib.CountdownDates
import com.madebykunal.doon.lib.CountdownMath
import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.HabitMath
import com.madebykunal.doon.lib.ParseRepeat
import com.madebykunal.doon.lib.Recurrence
import com.madebykunal.doon.lib.RepeatRule
import com.madebykunal.doon.lib.parseTime
import com.madebykunal.doon.notify.Reminders
import com.madebykunal.doon.state.DialogAction
import com.madebykunal.doon.state.Dialogs
import com.madebykunal.doon.state.Nav
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.state.SheetEntry
import com.madebykunal.doon.state.SheetRoute
import com.madebykunal.doon.state.TodoActions
import com.madebykunal.doon.state.TodoDetails
import com.madebykunal.doon.state.Ui
import com.madebykunal.doon.ui.components.DatePicker
import com.madebykunal.doon.ui.components.Field
import com.madebykunal.doon.ui.components.Ic
import com.madebykunal.doon.ui.components.Icon
import com.madebykunal.doon.ui.components.LocalSheetClose
import com.madebykunal.doon.ui.components.OptionRow
import com.madebykunal.doon.ui.components.SectionLabel
import com.madebykunal.doon.ui.components.SheetFrame
import com.madebykunal.doon.ui.components.SwitchOptionRow
import com.madebykunal.doon.ui.components.TimePicker
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.hairlineBottom
import com.madebykunal.doon.ui.components.tap
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.HIT_TARGET
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import com.madebykunal.doon.ui.theme.highlightIds

private val RULE_PRESETS = listOf(
    "Every day" to RepeatRule("daily", 1),
    "Every weekday" to RepeatRule("weekly", 1, listOf(1, 2, 3, 4, 5)),
    "Every week" to RepeatRule("weekly", 1),
    "Every month" to RepeatRule("monthly", 1),
    "Every year" to RepeatRule("yearly", 1),
)

private val TIME_PRESETS = listOf("Morning" to "09:00", "Noon" to "12:00", "Afternoon" to "15:00", "Evening" to "18:00")

@Composable
fun SheetContent(entry: SheetEntry) {
    when (val r = entry.route) {
        is SheetRoute.TodoDetails -> TodoDetailsSheet(r.id)
        is SheetRoute.MoveTo -> MoveToSheet(r.id, r.fromDate)
        is SheetRoute.ListEditor -> ListEditorSheet(r.listId, r.groupId, r.mode)
        is SheetRoute.CountdownEditor -> CountdownEditorSheet(r.id)
        is SheetRoute.HabitEditor -> HabitEditorSheet(r.id)
        is SheetRoute.Emoji -> EmojiSheet(r.onPick)
    }
}

@Composable
private fun Value(text: String) {
    Txt(text, Type.secondary, colors.inkMuted, Modifier.widthIn(max = 190.dp), maxLines = 1)
}

@Composable
private fun TodoDetailsSheet(id: String) {
    val close = LocalSheetClose.current
    val today = Ui.today
    val version = Live.version(T.TODOS)
    val live = remember(version, id) { TodoRepo.get(id) }
    val initial = remember { TodoRepo.get(id) }
    val openedAt = remember { System.currentTimeMillis() }
    val currentRule = remember { initial?.seriesId?.let { SeriesRepo.get(it)?.rule } }
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var highlight by remember { mutableStateOf(initial?.highlight) }
    var reminder by remember {
        mutableStateOf(initial?.remindAt?.let { Dates.dateOfMillis(it) to Dates.formatMillis(it, "HH:mm") })
    }
    var rule by remember { mutableStateOf(currentRule) }
    var phrase by remember { mutableStateOf("") }
    var panel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(live) { if (live == null || live.deletedAt != null) close() }
    if (initial == null || live == null || live.deletedAt != null) return

    val typed = if (phrase.isNotEmpty()) ParseRepeat.ruleOnly(phrase) else null
    val chosenRule = if (phrase.isNotEmpty()) typed ?: rule else rule
    val remindAt = reminder?.let { (d, t) -> parseTime(t).let { (h, m) -> Dates.toMillis(d, h, m) } }
    val inPast = remindAt != null && remindAt != initial.remindAt && remindAt <= openedAt
    fun toggle(next: String) {
        panel = if (panel == next) null else next
    }

    val save = {
        close()
        TodoActions.saveDetails(id, TodoDetails(text, description, highlight, remindAt, chosenRule), currentRule)
    }
    fun leave(action: (String) -> Unit) {
        close()
        action(id)
    }
    val place = live.date?.let { Dates.format(it, "EEE d MMM") } ?: live.listId?.let { ListRepo.get(it)?.name } ?: "List"

    SheetFrame("To-do", tall = true, action = Triple("Save", text.isNotBlank() && !inPast, save)) {
        Column(Modifier.padding(horizontal = GUTTER).padding(bottom = Space.m), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Field(text, { text = it }, "To-do", Modifier.fillMaxWidth(), Type.body.copy(fontWeight = FontWeight.SemiBold), singleLine = false)
            Field(description, { description = it }, "Description", Modifier.fillMaxWidth(), singleLine = false, minHeight = 96.dp, maxLength = 2000)
        }
        OptionRow("Reminder", right = { Value(reminder?.let { "${Dates.format(it.first, "EEE d MMM")} · ${it.second}" } ?: "None") }) {
            if (reminder == null) reminder = (live.date ?: today) to "09:00"
            toggle("reminder")
        }
        val r = reminder
        if (panel == "reminder" && r != null) {
            Column(Modifier.padding(bottom = Space.s)) {
                if (live.date == null) DatePicker(r.first, { reminder = it to r.second })
                TimePicker(r.second) { reminder = r.first to it }
                if (inPast) Txt("That time has already passed.", Type.caption, colors.inkMuted, Modifier.padding(horizontal = GUTTER, vertical = Space.xs))
                TIME_PRESETS.forEach { (label, time) ->
                    OptionRow(label, right = { Value(time) }, selected = r.second == time) { reminder = r.first to time }
                }
                OptionRow("No reminder", destructive = true) {
                    reminder = null
                    panel = null
                }
            }
        }
        if (live.date != null) {
            OptionRow("Repeat", right = { Value(chosenRule?.let(Recurrence::describe) ?: "Never") }) { toggle("repeat") }
            if (panel == "repeat") {
                Column(Modifier.padding(bottom = Space.s)) {
                    Field(phrase, { phrase = it }, "Type a rule, like every mon and thu", Modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(top = Space.s), capitalize = false)
                    if (phrase.isNotEmpty() && typed == null) {
                        Txt("Not understood yet. Try “every 2 weeks” or “every 15th”.", Type.caption, colors.inkMuted, Modifier.padding(horizontal = GUTTER, vertical = Space.xs))
                    }
                    RULE_PRESETS.forEach { (label, preset) ->
                        OptionRow(label, selected = phrase.isEmpty() && rule == preset) {
                            phrase = ""
                            rule = preset
                        }
                    }
                    OptionRow("Never", selected = phrase.isEmpty() && rule == null) {
                        phrase = ""
                        rule = null
                    }
                }
            }
        }
        HighlightRow(highlight) { highlight = it }
        OptionRow(if (live.date != null) "Day" else "List", right = { Value(place) }) { Nav.openSheet(SheetRoute.MoveTo(id = id)) }
        OptionRow("Turn into a heading") { leave(TodoActions::toggleHeading) }
        OptionRow("Delete", destructive = true) { leave(TodoActions::delete) }
    }
}

@Composable
private fun HighlightRow(value: String?, onChange: (String?) -> Unit) {
    val c = colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).hairlineBottom(c.hairline).padding(start = GUTTER, end = GUTTER - Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt("Highlight", modifier = Modifier.weight(1f))
        highlightIds.forEach { id ->
            Box(Modifier.size(HIT_TARGET - 4.dp, HIT_TARGET).tap(label = "Highlight $id") { onChange(id) }, contentAlignment = Alignment.Center) {
                var m = Modifier.size(24.dp).clip(CircleShape).background(c.highlightBar(id))
                if (value == id) m = m.border(2.dp, c.ink, CircleShape)
                Box(m)
            }
        }
        Box(Modifier.size(HIT_TARGET - 4.dp, HIT_TARGET).tap(label = "No highlight") { onChange(null) }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(24.dp).border(1.5.dp, if (value == null) c.ink else c.hairline, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Ic.X, c.inkMuted, 12.dp)
            }
        }
    }
}

@Composable
private fun MoveToSheet(id: String?, fromDate: DateStr?) {
    val close = LocalSheetClose.current
    val today = Ui.today
    val groups = remember { ListRepo.groups() }
    val lists = remember { ListRepo.lists() }
    fun go(home: Home, listName: String? = null) {
        if (id != null) TodoActions.moveTo(id, home, listName) else if (fromDate != null) TodoActions.moveUnfinished(fromDate, home, listName)
        close()
    }
    val tomorrow = Dates.addDays(today, 1)
    SheetFrame(if (fromDate != null) "Move unfinished to…" else "Move to…") {
        OptionRow("Today", Dates.format(today, "EEE, d MMM")) { go(Home.Day(today)) }
        OptionRow("Tomorrow", Dates.format(tomorrow, "EEE, d MMM")) { go(Home.Day(tomorrow)) }
        SectionLabel("Pick a date")
        DatePicker(null, { go(Home.Day(it)) })
        groups.forEach { group ->
            val inGroup = lists.filter { it.groupId == group.id }
            if (inGroup.isEmpty()) return@forEach
            SectionLabel(group.name)
            inGroup.forEach { list ->
                OptionRow(list.name, icon = list.emoji?.let { e -> { Txt(e) } }) { go(Home.InList(list.id), list.name) }
            }
        }
    }
}

@Composable
private fun NameRow(emoji: String?, onEmoji: ((String) -> Unit)?, content: @Composable () -> Unit) {
    val c = colors
    Row(Modifier.fillMaxWidth().padding(horizontal = GUTTER, vertical = Space.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        if (onEmoji != null) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(c.accentTint).tap(label = "Change emoji") { Nav.openSheet(SheetRoute.Emoji(onEmoji)) },
                contentAlignment = Alignment.Center,
            ) { Txt(emoji ?: "＋", Type.h2, unscaled = true) }
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun ListEditorSheet(listId: String?, groupId: String?, mode: String?) {
    val close = LocalSheetClose.current
    val groups = remember { ListRepo.groups() }
    val isGroup = mode == "group" || (groupId != null && mode != "list")
    val existingGroup = if (isGroup) groups.firstOrNull { it.id == groupId } else null
    val existingList = remember { listId?.let { ListRepo.get(it) } }
    var name by remember { mutableStateOf(existingList?.name ?: existingGroup?.name ?: "") }
    var emoji by remember { mutableStateOf(existingList?.emoji) }
    var chosenGroup by remember { mutableStateOf(existingList?.groupId ?: if (mode == "list") groupId else null) }
    val targetGroup = chosenGroup ?: groups.firstOrNull()?.id
    val editing = existingList != null || existingGroup != null
    val noun = if (isGroup) "group" else "list"

    val save = save@{
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@save
        if (isGroup) {
            if (existingGroup != null) ListRepo.updateGroup(existingGroup.id, "name" to trimmed) else ListRepo.createGroup(trimmed)
        } else if (existingList != null) {
            ListRepo.updateList(existingList.id, "name" to trimmed, "emoji" to emoji)
        } else if (targetGroup != null) {
            ListRepo.createList(trimmed, targetGroup, emoji)
        }
        close()
    }
    val remove = {
        Dialogs.show(
            "Delete this $noun?",
            "Everything in it is deleted too.",
            listOf(
                DialogAction("Cancel", "cancel"),
                DialogAction("Delete", "destructive") {
                    if (existingGroup != null) ListRepo.deleteGroup(existingGroup.id)
                    if (existingList != null) {
                        ListRepo.deleteList(existingList.id)
                        Nav.open(1)
                    }
                    close()
                },
            ),
        )
    }

    SheetFrame(if (editing) "Edit $noun" else "New $noun", action = Triple("Save", name.isNotBlank(), save)) {
        NameRow(emoji, if (isGroup) null else { e -> emoji = e }) {
            Field(name, { name = it }, if (isGroup) "Group name" else "List name", Modifier.fillMaxWidth(), autoFocus = !editing, onSubmit = save)
        }
        if (!isGroup && existingList == null && groups.size > 1) {
            SectionLabel("Group")
            groups.forEach { g -> OptionRow(g.name, selected = g.id == targetGroup) { chosenGroup = g.id } }
        }
        if (editing) OptionRow("Delete $noun", destructive = true, onClick = remove)
    }
}

private val COUNTDOWN_KINDS = listOf(
    Triple("until", "Until", "A one-off date"),
    Triple("yearly", "Yearly", "Birthdays, anniversaries. Shows the age they’ll turn"),
    Triple("monthly", "Monthly", "Rent day, pay day"),
    Triple("since", "Since", "Counts up from a date"),
)

@Composable
private fun CountdownEditorSheet(id: String?) {
    val c = colors
    val close = LocalSheetClose.current
    val today = Ui.today
    val existing = remember { id?.let { CountdownRepo.get(it) } }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "🎉") }
    var kind by remember { mutableStateOf(existing?.kind ?: "until") }
    var targetDate by remember { mutableStateOf(existing?.targetDate ?: today) }
    var hasTime by remember { mutableStateOf(existing?.targetTime != null) }
    var time by remember { mutableStateOf(existing?.targetTime ?: "09:00") }
    var showInWidget by remember { mutableStateOf(existing?.showInWidget ?: true) }
    var notify by remember { mutableStateOf(existing?.notify ?: Prefs.defaultCountdownAlerts) }

    val birthYear = if (existing != null && targetDate == existing.targetDate) existing.birthYear else null
    val draft = CountdownDates(kind, targetDate, birthYear)
    val counted = CountdownMath.display(draft, today)

    val save = save@{
        val name = title.trim()
        if (name.isEmpty()) return@save
        val targetTime = if (hasTime && kind == "until") time else null
        val alerts = if (kind == "since") emptyList() else notify
        val savedId = if (existing != null) {
            CountdownRepo.update(
                existing.id, "title" to name, "emoji" to emoji, "kind" to kind, "target_date" to targetDate, "target_time" to targetTime,
                "show_in_widget" to showInWidget, "notify" to org.json.JSONArray(alerts).toString(), "birth_year" to birthYear,
            )
            existing.id
        } else CountdownRepo.create(name, emoji, kind, targetDate, targetTime, showInWidget, alerts)
        Reminders.syncCountdown(savedId, ask = true)
        close()
    }

    SheetFrame(if (existing != null) "Edit countdown" else "New countdown", tall = true, action = Triple("Save", title.isNotBlank(), save)) {
        NameRow(emoji, { emoji = it }) { Field(title, { title = it }, "Title", Modifier.fillMaxWidth(), autoFocus = existing == null) }
        SectionLabel("Type")
        COUNTDOWN_KINDS.forEach { (k, label, help) -> OptionRow(label, help, selected = kind == k) { kind = k } }
        SectionLabel(if (kind == "since") "Started on" else "Date")
        DatePicker(targetDate, {
            targetDate = it
            if (kind == "until" && it < today) kind = "yearly"
        })
        Row(
            Modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(top = Space.s, bottom = Space.xs).clip(RoundedCornerShape(12.dp)).background(c.accentTint)
                .padding(horizontal = Space.m, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Txt(counted.value + if (counted.unit.isNotEmpty()) " ${counted.unit}" else "", Type.secondaryStrong, c.accentText, tabularNums = true)
            Txt(
                "${CountdownMath.dateLine(draft, today)} ${CountdownMath.titleWithAge(draft, "", today).trim()}".trim(),
                Type.caption, c.accentText, Modifier.weight(1f), maxLines = 2, align = TextAlign.End,
            )
        }
        if (kind == "until") {
            SwitchOptionRow("Add a time", hasTime) { hasTime = it }
            if (hasTime) Box(Modifier.padding(vertical = Space.s)) { TimePicker(time) { time = it } }
        }
        SectionLabel("Show")
        SwitchOptionRow("In the home-screen widget", showInWidget) { showInWidget = it }
        if (kind != "since") {
            SectionLabel("Notify me")
            CountdownMath.alerts.forEach { (offset, label) ->
                SwitchOptionRow(label, offset in notify) { notify = if (offset in notify) notify - offset else notify + offset }
            }
        }
    }
}

private const val MAX_TARGET = 20

@Composable
private fun HabitEditorSheet(id: String?) {
    val c = colors
    val close = LocalSheetClose.current
    val today = Ui.today
    val existing = remember { id?.let { HabitRepo.get(it) } }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "🌱") }
    var kind by remember { mutableStateOf(existing?.kind ?: "build") }
    var target by remember { mutableIntStateOf(existing?.target ?: 1) }
    var days by remember { mutableIntStateOf(existing?.days ?: HabitMath.EVERY_DAY) }
    var startDate by remember { mutableStateOf(existing?.startDate ?: today) }

    val save = save@{
        val name = title.trim()
        if (name.isEmpty()) return@save
        val t = if (kind == "build") target else 1
        if (existing != null) HabitRepo.update(existing.id, "title" to name, "emoji" to emoji, "kind" to kind, "target" to t, "days" to days, "start_date" to startDate)
        else HabitRepo.create(name, emoji, kind, t, days, startDate)
        close()
    }

    SheetFrame(if (existing != null) "Edit habit" else "New habit", tall = true, action = Triple("Save", title.isNotBlank(), save)) {
        NameRow(emoji, { emoji = it }) { Field(title, { title = it }, "Habit name", Modifier.fillMaxWidth(), autoFocus = existing == null) }
        if (existing == null) {
            SectionLabel("Type")
            OptionRow("Build", "Something to do more often", selected = kind == "build") { kind = "build" }
            OptionRow("Quit", "Something to stop. Counts the days free", selected = kind == "quit") { kind = "quit" }
        }
        if (kind == "build") {
            SectionLabel("Days")
            Row(Modifier.fillMaxWidth().padding(horizontal = GUTTER, vertical = Space.s), horizontalArrangement = Arrangement.SpaceBetween) {
                (0 until 7).map { (Prefs.weekStart + it) % 7 }.forEach { weekday ->
                    val on = days and (1 shl weekday) != 0
                    val name = Dates.format(Dates.addDays("2026-11-01", weekday), "EEEE")
                    var m = Modifier.size(40.dp).clip(CircleShape)
                    m = if (on) m.background(c.accent) else m.border(1.dp, c.hairline, CircleShape)
                    Box(
                        m.tap(label = name) {
                            val next = days xor (1 shl weekday)
                            if (next != 0) days = next
                        },
                        contentAlignment = Alignment.Center,
                    ) { Txt(name.take(1), Type.secondaryStrong, if (on) c.onAccent else c.inkMuted) }
                }
            }
            SectionLabel("Times a day")
            OptionRow(if (target == 1) "Once" else "$target times", right = {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    StepButton(Ic.Minus, "Fewer", target > 1) { target = maxOf(1, target - 1) }
                    StepButton(Ic.Plus, "More", target < MAX_TARGET) { target = minOf(MAX_TARGET, target + 1) }
                }
            })
        } else {
            SectionLabel("Free since")
            DatePicker(startDate, { startDate = if (it > today) today else it })
        }
    }
}

@Composable
private fun StepButton(ic: Ic, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = colors
    Box(
        Modifier.size(HIT_TARGET - 4.dp).alpha(if (enabled) 1f else 0.4f).clip(CircleShape).background(c.accentTint).tap(enabled = enabled, label = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(ic, c.accentText, 18.dp, stroke = 2.25f) }
}

private val COMMON_EMOJI = listOf(
    "🎂", "🎉", "🎁", "🎈", "💍", "❤️", "👶", "🎓", "🏆", "⭐",
    "✈️", "🌴", "🏖️", "🏔️", "🗺️", "🚗", "🚆", "🏠", "🔑", "🛋️",
    "💼", "📅", "📌", "📝", "💰", "💳", "🧾", "📦", "🛒", "🍽️",
    "☕", "🍕", "🥂", "🍰", "🏃", "🚴", "🧘", "💪", "⚽", "🎾",
    "🎬", "🎵", "🎸", "🎮", "📚", "🎨", "📷", "🌱", "🌸", "🌞",
    "🌙", "❄️", "🎄", "🎃", "🪔", "🐶", "🐾", "💊", "🦷", "🧠",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiSheet(onPick: (String) -> Unit) {
    val close = LocalSheetClose.current
    var typed by remember { mutableStateOf("") }
    fun choose(emoji: String) {
        onPick(emoji)
        close()
    }
    SheetFrame("Emoji") {
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = Space.m), horizontalArrangement = Arrangement.Center) {
            COMMON_EMOJI.forEach { e ->
                Box(Modifier.size(HIT_TARGET + 8.dp).tap(label = e) { choose(e) }, contentAlignment = Alignment.Center) { Txt(e, Type.h2, unscaled = true) }
            }
        }
        Field(typed, { text ->
            typed = text
            val trimmed = text.trim()
            if (trimmed.isNotEmpty()) {
                val end = trimmed.offsetByCodePoints(0, minOf(2, trimmed.codePointCount(0, trimmed.length)))
                choose(trimmed.substring(0, end))
            }
        }, "Or type any emoji", Modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(top = Space.l))
    }
}
