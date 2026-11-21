package com.madebykunal.doon.ui.sheets

import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.sp
import com.madebykunal.doon.ui.components.Checkbox
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import com.madebykunal.doon.ui.components.Choice
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
        SheetRoute.ReorderFolders -> ReorderFoldersSheet()
    }
}

@Composable
private fun Value(text: String) {
    Txt(text, Type.secondary, colors.inkMuted, Modifier.widthIn(max = 190.dp), maxLines = 1)
}

@Composable
private fun PlainField(value: String, onChange: (String) -> Unit, placeholder: String, style: TextStyle, modifier: Modifier = Modifier) {
    val c = colors
    BasicTextField(
        value = value,
        onValueChange = { onChange(it.take(2000)) },
        modifier = modifier,
        textStyle = style.copy(color = c.ink),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Txt(placeholder, style, c.inkMuted)
                inner()
            }
        },
    )
}

@Composable
private fun DetailRow(ic: Ic, label: String, value: String? = null, tint: Color? = null, onClick: () -> Unit) {
    val c = colors
    Row(
        Modifier.fillMaxWidth().tap(pressedColor = c.hairline, label = label, onClick = onClick).hairlineBottom(c.hairline)
            .heightIn(min = 56.dp).padding(horizontal = GUTTER),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        Icon(ic, tint ?: c.inkMuted, 20.dp)
        Txt(label, color = tint ?: c.ink, modifier = Modifier.weight(1f))
        if (value != null) Txt(value, Type.secondary, if (value == "None" || value == "Never") c.inkMuted else c.accentText, Modifier.widthIn(max = 200.dp), maxLines = 1)
    }
}

@Composable
private fun TodoDetailsSheet(id: String) {
    val c = colors
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
    val place = live.date?.let {
        when (it) {
            today -> "Today"
            Dates.addDays(today, 1) -> "Tomorrow"
            else -> Dates.format(it, "EEEE, d MMMM")
        }
    } ?: live.listId?.let { ListRepo.get(it)?.name } ?: "List"

    SheetFrame("", expandable = true, action = Triple("Save", text.isNotBlank() && !inPast, save)) {
        Txt(place.uppercase(), Type.label, c.inkMuted, Modifier.padding(horizontal = GUTTER).padding(bottom = Space.s))
        Row(Modifier.fillMaxWidth().padding(start = GUTTER - 11.dp, end = GUTTER), verticalAlignment = Alignment.Top) {
            Checkbox(live.completed) { TodoActions.toggle(id) }
            PlainField(text, { text = it }, "Task name", Type.h2.copy(fontWeight = FontWeight.Medium), Modifier.weight(1f).padding(top = 10.dp))
        }
        Row(
            Modifier.fillMaxWidth().hairlineBottom(c.hairline).padding(start = GUTTER, end = GUTTER, top = Space.s, bottom = Space.l),
            horizontalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            Icon(Ic.TextLines, c.inkMuted, 20.dp, modifier = Modifier.padding(top = 1.dp))
            PlainField(description, { description = it }, "Add a description", Type.secondary.copy(lineHeight = 21.sp), Modifier.weight(1f).heightIn(min = 48.dp))
        }

        DetailRow(Ic.Bell, "Reminder", reminder?.let { "${Dates.format(it.first, "EEE d MMM")} · ${it.second}" } ?: "None") {
            if (reminder == null) reminder = (live.date ?: today) to "09:00"
            toggle("reminder")
        }
        val r = reminder
        if (panel == "reminder" && r != null) {
            Column(Modifier.padding(bottom = Space.s)) {
                if (live.date == null) DatePicker(r.first, { reminder = it to r.second })
                TimePicker(r.second) { reminder = r.first to it }
                if (inPast) Txt("That time has already passed.", Type.caption, c.inkMuted, Modifier.padding(horizontal = GUTTER, vertical = Space.xs))
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
            DetailRow(Ic.Repeat, "Repeat", chosenRule?.let { Recurrence.describe(it).removePrefix("Repeats ").replaceFirstChar(Char::uppercase) } ?: "Never") { toggle("repeat") }
            if (panel == "repeat") {
                Column(Modifier.padding(bottom = Space.s)) {
                    Field(phrase, { phrase = it }, "Type a rule, like every mon and thu", Modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(top = Space.s), capitalize = false)
                    if (phrase.isNotEmpty() && typed == null) {
                        Txt("Not understood yet. Try “every 2 weeks” or “every 15th”.", Type.caption, c.inkMuted, Modifier.padding(horizontal = GUTTER, vertical = Space.xs))
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
        Row(
            Modifier.fillMaxWidth().hairlineBottom(c.hairline).heightIn(min = 56.dp).padding(start = GUTTER, end = GUTTER - Space.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            Box(Modifier.size(20.dp).background(highlight?.let { c.highlightBar(it) } ?: Color.Transparent, CircleShape).border(1.5.dp, c.inkMuted, CircleShape))
            Txt("Highlight", modifier = Modifier.weight(1f))
            highlightIds.forEach { hid ->
                Box(Modifier.size(HIT_TARGET - 8.dp, HIT_TARGET).tap(label = "Highlight $hid") { highlight = if (highlight == hid) null else hid }, contentAlignment = Alignment.Center) {
                    var m = Modifier.size(22.dp).background(c.highlightBar(hid), CircleShape)
                    if (highlight == hid) m = m.border(2.dp, c.ink, CircleShape)
                    Box(m)
                }
            }
        }
        DetailRow(Ic.Trash, "Delete task", tint = c.dangerText) {
            close()
            TodoActions.delete(id)
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
                OptionRow(list.name) { go(Home.InList(list.id), list.name) }
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
                Modifier.size(48.dp).clip(RectangleShape).background(c.accentTint).tap(label = "Change emoji") { Nav.openSheet(SheetRoute.Emoji(onEmoji)) },
                contentAlignment = Alignment.Center,
            ) { Txt(emoji ?: "＋", Type.h2, unscaled = true) }
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun ListEditorSheet(listId: String?, groupId: String?, mode: String?) {
    val c = colors
    val close = LocalSheetClose.current
    val groups = remember { ListRepo.groups() }
    val isGroup = mode == "group" || (groupId != null && mode != "list")
    val existingGroup = if (isGroup) groups.firstOrNull { it.id == groupId } else null
    val existingList = remember { listId?.let { ListRepo.get(it) } }
    var name by remember { mutableStateOf(existingList?.name ?: existingGroup?.name ?: "") }
    var chosenGroup by remember { mutableStateOf(existingList?.groupId ?: if (mode == "list") groupId else null) }
    val targetGroup = chosenGroup ?: groups.firstOrNull()?.id
    val editing = existingList != null || existingGroup != null
    val noun = if (isGroup) "folder" else "list"
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val save = save@{
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@save
        if (isGroup) {
            if (existingGroup != null) ListRepo.updateGroup(existingGroup.id, "name" to trimmed) else ListRepo.createGroup(trimmed)
        } else if (existingList != null) {
            ListRepo.updateList(existingList.id, "name" to trimmed)
        } else if (targetGroup != null) {
            ListRepo.createList(trimmed, targetGroup)
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

    Column(Modifier.fillMaxWidth().background(c.surface).navigationBarsPadding().padding(top = Space.xl, bottom = Space.l)) {
        Txt((if (editing) "Rename $noun" else "New $noun").uppercase(), Type.label, c.inkMuted, Modifier.padding(horizontal = GUTTER))
        BasicTextField(
            value = name,
            onValueChange = { name = it.replace("\n", "") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(top = Space.s).focusRequester(focus),
            textStyle = Type.listName.copy(color = c.ink),
            singleLine = true,
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            decorationBox = { inner ->
                Column {
                    Box(Modifier.padding(vertical = Space.s)) {
                        if (name.isEmpty()) Txt((if (isGroup) "Folder name" else "List name").uppercase(), Type.listName, c.inkMuted)
                        inner()
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(c.accent))
                }
            },
        )
        if (!isGroup && existingList == null && groups.size > 1) {
            Txt("FOLDER", Type.label, c.inkMuted, Modifier.padding(horizontal = GUTTER).padding(top = Space.xl, bottom = Space.s))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = GUTTER), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                groups.forEach { g ->
                    val selected = g.id == targetGroup
                    Box(
                        Modifier.height(40.dp).background(if (selected) c.accent else c.hairline).tap(label = g.name) { chosenGroup = g.id }.padding(horizontal = Space.m),
                        contentAlignment = Alignment.Center,
                    ) { Txt(g.name.uppercase(), Type.nav.copy(fontSize = 17.sp, lineHeight = 20.sp), if (selected) c.onAccent else c.ink, maxLines = 1, unscaled = true) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = GUTTER, end = GUTTER, top = Space.xl), verticalAlignment = Alignment.CenterVertically) {
            if (editing) Txt("DELETE", Type.nav.copy(fontSize = 18.sp), c.dangerText, Modifier.tap(label = "Delete $noun", onClick = remove).padding(vertical = Space.s), unscaled = true)
            Spacer(Modifier.weight(1f))
            Txt("CANCEL", Type.nav.copy(fontSize = 18.sp), c.inkMuted, Modifier.tap(label = "Cancel", onClick = close).padding(Space.s), unscaled = true)
            Box(
                Modifier.padding(start = Space.s).height(44.dp).background(if (name.isBlank()) c.hairline else c.accent).tap(enabled = name.isNotBlank(), label = "Save") { save() }.padding(horizontal = Space.xl),
                contentAlignment = Alignment.Center,
            ) { Txt("SAVE", Type.nav.copy(fontSize = 18.sp), if (name.isBlank()) c.inkMuted else c.onAccent, unscaled = true) }
        }
    }
}

@Composable
private fun ReorderFoldersSheet() {
    val c = colors
    val close = LocalSheetClose.current
    val version = Live.version(T.GROUPS)
    val groups = remember(version) { ListRepo.groups() }
    val rowPx = with(LocalDensity.current) { 60.dp.toPx() }
    var dragging by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableStateOf(0f) }
    val from = groups.indexOfFirst { it.id == dragging }
    val to = if (from < 0) -1 else (from + (offset / rowPx).roundToInt()).coerceIn(0, groups.size - 1)
    Column(Modifier.fillMaxWidth().background(c.surface).navigationBarsPadding().padding(top = Space.xl, bottom = Space.l)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(bottom = Space.s), verticalAlignment = Alignment.CenterVertically) {
            Txt("REORDER FOLDERS", Type.label, c.inkMuted, Modifier.weight(1f))
            Txt("DONE", Type.nav.copy(fontSize = 18.sp), c.accentText, Modifier.tap(label = "Done", onClick = close).padding(Space.s), unscaled = true)
        }
        groups.forEachIndexed { index, group ->
            val shift = when {
                from < 0 || group.id == dragging -> 0f
                index in (from + 1)..to -> -rowPx
                index in to until from -> rowPx
                else -> 0f
            }
            val animated by animateFloatAsState(shift, label = "shift")
            val isDragged = group.id == dragging
            Row(
                Modifier.fillMaxWidth().zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragged) offset else animated }
                    .then(if (isDragged) Modifier.shadow(8.dp) else Modifier)
                    .background(if (isDragged) c.bg else c.surface).hairlineBottom(c.hairline).height(60.dp)
                    .pointerInput(group.id) {
                        detectDragGestures(
                            onDragStart = {
                                dragging = group.id
                                offset = 0f
                            },
                            onDragEnd = {
                                val f = groups.indexOfFirst { it.id == group.id }
                                val t = (f + (offset / rowPx).roundToInt()).coerceIn(0, groups.size - 1)
                                if (f >= 0 && t != f) {
                                    val rest = groups.filter { it.id != group.id }
                                    ListRepo.reorderGroup(group.id, rest.getOrNull(t - 1)?.position, rest.getOrNull(t)?.position)
                                }
                                dragging = null
                                offset = 0f
                            },
                            onDragCancel = {
                                dragging = null
                                offset = 0f
                            },
                        ) { change, amount ->
                            change.consume()
                            offset += amount.y
                        }
                    }
                    .padding(horizontal = GUTTER),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt(group.name.uppercase(), Type.listName.copy(fontSize = 22.sp), maxLines = 1, modifier = Modifier.weight(1f))
                Icon(Ic.Grip, c.inkMuted, 22.dp)
            }
        }
    }
}

private val COUNTDOWN_KINDS = listOf(
    Triple("until", "Once", "Counts down to one date"),
    Triple("yearly", "Yearly", "Comes back every year. Birthdays show the age they’ll turn"),
    Triple("monthly", "Monthly", "Comes back every month, like pay day"),
    Triple("since", "Since", "Counts up from a day that has passed"),
)

private val YEARLY_WORDS = listOf("birthday", "bday", "anniversary", "jayanti")

@Composable
private fun EditorLabel(text: String) {
    Txt(text.uppercase(), Type.label, colors.inkMuted, Modifier.padding(horizontal = GUTTER).padding(top = Space.xxl, bottom = Space.s))
}

@Composable
private fun CountdownEditorSheet(id: String?) {
    val c = colors
    val close = LocalSheetClose.current
    val today = Ui.today
    val existing = remember { id?.let { CountdownRepo.get(it) } }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    val emoji = existing?.emoji ?: "🎉"
    var kind by remember { mutableStateOf(existing?.kind ?: "until") }
    var kindChosen by remember { mutableStateOf(existing != null) }
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
    val ready = title.isNotBlank()

    SheetFrame(if (existing != null) "Edit countdown" else "New countdown", tall = true, action = Triple("Save", ready, save)) {
        Field(
            title,
            {
                title = it
                if (!kindChosen && kind == "until" && YEARLY_WORDS.any { w -> it.contains(w, ignoreCase = true) }) kind = "yearly"
            },
            "What’s coming up?",
            Modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(top = Space.s),
            style = Type.display.copy(fontSize = 26.sp, lineHeight = 32.sp),
            autoFocus = existing == null,
        )

        EditorLabel("Type")
        Row(Modifier.fillMaxWidth().padding(horizontal = GUTTER), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            COUNTDOWN_KINDS.forEach { (k, label, _) ->
                Choice(label, kind == k, Modifier.weight(1f)) {
                    kind = k
                    kindChosen = true
                }
            }
        }
        Txt(COUNTDOWN_KINDS.first { it.first == kind }.third, Type.caption, c.inkMuted, Modifier.padding(horizontal = GUTTER).padding(top = Space.s))

        Row(
            Modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(top = Space.xxl),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Txt(counted.value.removeSuffix(" 🎉").uppercase(), Type.display.copy(fontSize = 56.sp, lineHeight = 58.sp), c.accentText, maxLines = 1, tabularNums = true, unscaled = true)
            Column(Modifier.weight(1f).padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (counted.unit.isNotEmpty()) Txt(counted.unit.uppercase(), Type.nav, c.ink, maxLines = 1, unscaled = true)
                Txt("${CountdownMath.dateLine(draft, today)} ${CountdownMath.titleWithAge(draft, "", today).trim()}".trim(), Type.caption, c.inkMuted, maxLines = 2)
            }
        }

        EditorLabel(if (kind == "since") "Started on" else "Date")
        DatePicker(targetDate, {
            targetDate = it
            if (kind == "until" && it < today) kind = "yearly"
        })
        if (kind == "until") {
            SwitchOptionRow("Add a time", hasTime) { hasTime = it }
            if (hasTime) Box(Modifier.padding(vertical = Space.s)) { TimePicker(time) { time = it } }
        }

        if (kind != "since") {
            EditorLabel("Remind me")
            Row(Modifier.fillMaxWidth().padding(horizontal = GUTTER), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CountdownMath.alerts.forEach { (offset, label) ->
                    Choice(label, offset in notify, Modifier.weight(1f)) { notify = if (offset in notify) notify - offset else notify + offset }
                }
            }
        }

        Box(Modifier.padding(top = Space.l)) {
            SwitchOptionRow("Show on the home-screen widget", showInWidget) { showInWidget = it }
        }

        Box(
            Modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(top = Space.l).heightIn(min = 56.dp)
                .background(if (ready) c.accent else c.hairline).tap(enabled = ready, label = "Save", onClick = save),
            contentAlignment = Alignment.Center,
        ) {
            Txt(if (existing != null) "SAVE CHANGES" else "ADD COUNTDOWN", Type.nav, if (ready) c.onAccent else c.inkMuted, unscaled = true)
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
                    var m = Modifier.size(40.dp).clip(RectangleShape)
                    m = if (on) m.background(c.accent) else m.border(1.dp, c.hairline, RectangleShape)
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
        Modifier.size(HIT_TARGET - 4.dp).alpha(if (enabled) 1f else 0.4f).clip(RectangleShape).background(c.accentTint).tap(enabled = enabled, label = label, onClick = onClick),
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
