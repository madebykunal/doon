package com.madebykunal.doon.state

import com.madebykunal.doon.data.Home
import com.madebykunal.doon.data.SeriesRepo
import com.madebykunal.doon.data.Todo
import com.madebykunal.doon.data.TodoRepo
import com.madebykunal.doon.data.Where
import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.Markdown
import com.madebykunal.doon.lib.SmartInput
import com.madebykunal.doon.lib.parseTime
import com.madebykunal.doon.lib.Recurrence
import com.madebykunal.doon.lib.RepeatRule
import com.madebykunal.doon.notify.Reminders

data class TodoDetails(val text: String, val description: String?, val highlight: String?, val remindAt: Long?, val rule: RepeatRule?)

object TodoActions {
    private fun homeLabel(home: Home, listName: String?): String = when (home) {
        is Home.InList -> listName ?: "list"
        is Home.Day -> when (home.date) {
            Ui.today -> "today"
            Dates.addDays(Ui.today, 1) -> "tomorrow"
            else -> Dates.format(home.date, "EEE, d MMM")
        }
    }

    fun toggle(id: String) {
        val todo = TodoRepo.get(id) ?: return
        if (todo.completed) {
            TodoRepo.setCompleted(listOf(id), null)
            Reminders.syncTodo(id)
            return
        }
        TodoRepo.setCompleted(listOf(id), System.currentTimeMillis())
        Reminders.cancelTodo(id)
        if (todo.date != null && TodoRepo.countOpenOn(todo.date) == 0 && Prefs.celebration) Ui.cleared++
    }

    fun completeFromWidget(id: String) {
        val todo = TodoRepo.get(id) ?: return
        if (todo.completed || todo.deletedAt != null) return
        TodoRepo.setCompleted(listOf(id), System.currentTimeMillis())
        Reminders.cancelTodo(id)
    }

    fun completeAll(date: DateStr) {
        val ids = TodoRepo.items(Home.Day(date)).filter { !it.isHeading && !it.completed }.map { it.id }
        if (ids.isEmpty()) return
        TodoRepo.setCompleted(ids, System.currentTimeMillis())
        ids.forEach(Reminders::cancelTodo)
        Snack.show("Completed ${ids.size} ${if (ids.size == 1) "to-do" else "to-dos"}") {
            TodoRepo.setCompleted(ids, null)
            ids.forEach(Reminders::syncTodo)
        }
    }

    private fun softDeleteWithUndo(todo: Todo) {
        TodoRepo.softDelete(todo.id)
        Reminders.cancelTodo(todo.id)
        Snack.show("To-do deleted") {
            TodoRepo.restore(todo.id)
            Reminders.syncTodo(todo.id)
        }
    }

    fun delete(id: String) {
        val todo = TodoRepo.get(id) ?: return
        val seriesId = todo.seriesId
        val seriesDate = todo.seriesDate
        if (seriesId == null || seriesDate == null) {
            softDeleteWithUndo(todo)
            return
        }
        Dialogs.show(
            "Delete repeating to-do",
            Markdown.plainText(todo.text),
            listOf(
                DialogAction("Delete this one") {
                    Reminders.cancelTodo(id)
                    SeriesRepo.skipInstance(id, seriesId, seriesDate)
                },
                DialogAction("Delete series", "destructive") {
                    Reminders.cancelTodo(id)
                    SeriesRepo.delete(seriesId, seriesDate)
                },
                DialogAction("Cancel", "cancel"),
            ),
        )
    }

    private fun detachIfSeries(todo: Todo) {
        if (todo.seriesId != null && todo.seriesDate != null) SeriesRepo.detachInstance(todo.seriesId, todo.seriesDate)
    }

    fun moveTo(id: String, home: Home, listName: String? = null) {
        val todo = TodoRepo.get(id) ?: return
        val before = TodoRepo.placementOf(todo)
        detachIfSeries(todo)
        TodoRepo.move(id, home)
        Reminders.syncTodo(id)
        Snack.show("Moved to ${homeLabel(home, listName)}") {
            TodoRepo.restorePlacement(id, before)
            Reminders.syncTodo(id)
        }
    }

    fun moveToTomorrow(id: String) {
        val date = TodoRepo.get(id)?.date ?: return
        moveTo(id, Home.Day(Dates.addDays(date, 1)))
    }

    fun moveToToday(id: String) = moveTo(id, Home.Day(Ui.today))

    fun moveUnfinished(from: DateStr, home: Home, listName: String? = null) {
        val moved = TodoRepo.moveOpenItems(from, home)
        if (moved.isEmpty()) return
        Snack.show("Moved ${moved.size} to ${homeLabel(home, listName)}") {
            moved.forEach { (id, placement) -> TodoRepo.restorePlacement(id, placement) }
        }
    }

    fun setHighlight(id: String, highlight: String?) {
        val todo = TodoRepo.get(id) ?: return
        TodoRepo.update(id, "highlight" to highlight)
        detachIfSeries(todo)
    }

    private fun dayLabel(date: DateStr): String = when (date) {
        Ui.today -> "today"
        Dates.addDays(Ui.today, 1) -> "tomorrow"
        else -> Dates.format(date, "EEE, d MMM")
    }

    fun addFromInput(input: String, home: Home, where: Where, keepWords: Boolean = false, note: String = ""): Todo? {
        val text = input.trim()
        if (text.isEmpty()) return null
        val description = note.trim().ifEmpty { null }
        if (keepWords) return TodoRepo.create(home, text, description, where)
        val base = (home as? Home.Day)?.date ?: Ui.today
        val smart = SmartInput.parse(text, Ui.today)
        if (!smart.found) return TodoRepo.create(home, text, description, where)
        val title = smart.title.ifEmpty { text }
        val (hour, minute) = parseTime(smart.time)

        val rule = smart.rule
        if (rule != null) {
            val from = smart.date ?: base
            val start = Recurrence.expand(rule, from, null, from, Dates.addDays(from, 400)).firstOrNull() ?: from
            val series = SeriesRepo.create(title, rule, start, description = description)
            SeriesRepo.materialize(start, start)
            val todo = TodoRepo.items(Home.Day(start)).firstOrNull { it.seriesId == series.id }
            if (todo != null && smart.time != null) Reminders.setTodoReminder(todo.id, Dates.toMillis(start, hour, minute), ask = true)
            if (home != Home.Day(start)) Snack.show("Repeats from ${dayLabel(start)}")
            return if (home == Home.Day(start)) todo else null
        }

        val day = smart.date ?: base
        val target = if (home is Home.Day) Home.Day(day) else home
        val todo = TodoRepo.create(target, title, description, if (target == home) where else Where.Bottom)
        if (smart.time != null || home is Home.InList) Reminders.setTodoReminder(todo.id, Dates.toMillis(day, hour, minute), ask = true)
        if (target != home) Snack.show("Added to ${dayLabel(day)}")
        return if (target == home) todo else null
    }

    fun saveEdit(id: String, input: String, description: String? = null, keepDescription: Boolean = true) {
        val todo = TodoRepo.get(id) ?: return
        val text = input.trim()
        if (text.isEmpty()) {
            delete(id)
            return
        }
        val nextDescription = if (keepDescription) todo.description else description?.trim()?.ifEmpty { null }
        if (text == todo.text && nextDescription == todo.description) return

        if (todo.seriesId != null && todo.seriesDate != null) {
            val seriesId = todo.seriesId
            val seriesDate = todo.seriesDate
            Dialogs.show(
                "Edit repeating to-do",
                Markdown.plainText(text),
                listOf(
                    DialogAction("This one") {
                        SeriesRepo.detachInstance(seriesId, seriesDate)
                        TodoRepo.update(id, "text" to text, "description" to nextDescription)
                        Reminders.syncTodo(id)
                    },
                    DialogAction("All future", "primary") {
                        SeriesRepo.updateText(seriesId, seriesDate, "text" to text, "description" to nextDescription)
                    },
                    DialogAction("Cancel", "cancel"),
                ),
            )
            return
        }

        TodoRepo.update(id, "text" to text, "description" to nextDescription)
        Reminders.syncTodo(id)
    }

    fun saveDetails(id: String, next: TodoDetails, currentRule: RepeatRule?) {
        val todo = TodoRepo.get(id) ?: return
        if (next.highlight != todo.highlight) setHighlight(id, next.highlight)
        if (next.remindAt != todo.remindAt) Reminders.setTodoReminder(id, next.remindAt, ask = true)
        val ruleChanged = next.rule != currentRule
        val text = next.text.trim()
        val description = next.description?.trim()?.ifEmpty { null }
        if (ruleChanged && todo.seriesId != null && todo.seriesDate != null) {
            if (text != todo.text || description != todo.description) {
                SeriesRepo.updateText(todo.seriesId, todo.seriesDate, "text" to text, "description" to description)
            }
            applyRule(id, next.rule)
            return
        }
        saveEdit(id, text, description, keepDescription = false)
        if (ruleChanged) applyRule(id, next.rule)
    }

    private fun applyRule(id: String, rule: RepeatRule?) {
        val todo = TodoRepo.get(id) ?: return
        val date = todo.date ?: return
        if (todo.seriesId != null && todo.seriesDate != null) {
            if (rule != null) {
                SeriesRepo.changeRule(todo.seriesId, todo.seriesDate, rule)
                SeriesRepo.materialize(todo.seriesDate, todo.seriesDate)
            } else {
                TodoRepo.update(id, "series_id" to null, "series_date" to null)
                SeriesRepo.delete(todo.seriesId, todo.seriesDate)
            }
            return
        }
        if (rule != null) {
            val created = SeriesRepo.create(todo.text, rule, date, todo.highlight, todo.description)
            TodoRepo.update(id, "series_id" to created.id, "series_date" to date)
        }
    }
}
