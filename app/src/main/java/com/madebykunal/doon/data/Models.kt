package com.madebykunal.doon.data

import android.database.Cursor
import com.madebykunal.doon.lib.CountdownDates
import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.RepeatRule
import com.madebykunal.doon.lib.Schedule
import org.json.JSONArray

sealed interface Home {
    data class Day(val date: DateStr) : Home
    data class InList(val listId: String) : Home
}

data class Todo(
    val id: String,
    val text: String,
    val description: String?,
    val isHeading: Boolean,
    val listId: String?,
    val date: DateStr?,
    val position: String,
    val highlight: String?,
    val completedAt: Long?,
    val seriesId: String?,
    val seriesDate: DateStr?,
    val rolloverCount: Int,
    val remindAt: Long?,
    val notificationId: String?,
    val collapsed: Boolean,
    val createdAt: Long,
    val deletedAt: Long?,
) {
    val completed get() = completedAt != null

    companion object {
        fun from(c: Cursor) = Todo(
            id = c.str("id"),
            text = c.str("text"),
            description = c.optStr("description"),
            isHeading = c.bool("is_heading"),
            listId = c.strOrNull("list_id"),
            date = c.strOrNull("date"),
            position = c.str("position"),
            highlight = c.strOrNull("highlight"),
            completedAt = c.longOrNull("completed_at"),
            seriesId = c.strOrNull("series_id"),
            seriesDate = c.strOrNull("series_date"),
            rolloverCount = c.int("rollover_count"),
            remindAt = c.longOrNull("remind_at"),
            notificationId = c.strOrNull("notification_id"),
            collapsed = c.bool("collapsed"),
            createdAt = c.long("created_at"),
            deletedAt = c.longOrNull("deleted_at"),
        )
    }
}

data class Placement(val date: DateStr?, val listId: String?, val position: String)

data class Series(
    val id: String,
    val text: String,
    val description: String?,
    val rule: RepeatRule,
    val startDate: DateStr,
    val endDate: DateStr?,
    val highlight: String?,
) {
    companion object {
        fun from(c: Cursor): Series? {
            val rule = RepeatRule.fromJson(c.str("rule")) ?: return null
            return Series(c.str("id"), c.str("text"), c.optStr("description"), rule, c.str("start_date"), c.strOrNull("end_date"), c.strOrNull("highlight"))
        }
    }
}

data class ListGroup(val id: String, val name: String, val position: String, val collapsed: Boolean) {
    companion object {
        fun from(c: Cursor) = ListGroup(c.str("id"), c.str("name"), c.str("position"), c.bool("collapsed"))
    }
}

data class TodoList(val id: String, val name: String, val emoji: String?, val groupId: String, val position: String) {
    companion object {
        fun from(c: Cursor) = TodoList(c.str("id"), c.str("name"), c.strOrNull("emoji"), c.str("group_id"), c.str("position"))
    }
}

data class Countdown(
    val id: String,
    val title: String,
    val emoji: String,
    val kind: String,
    val targetDate: DateStr,
    val targetTime: String?,
    val birthYear: Int?,
    val showInWidget: Boolean,
    val notify: List<Int>,
    val notificationIds: List<String>,
    val archivedAt: Long?,
    val createdAt: Long,
    val deletedAt: Long?,
) {
    val dates get() = CountdownDates(kind, targetDate, birthYear)

    companion object {
        fun from(c: Cursor) = Countdown(
            id = c.str("id"),
            title = c.str("title"),
            emoji = c.str("emoji"),
            kind = c.str("kind"),
            targetDate = c.str("target_date"),
            targetTime = c.strOrNull("target_time"),
            birthYear = c.intOrNull("birth_year"),
            showInWidget = c.bool("show_in_widget"),
            notify = ints(c.str("notify")),
            notificationIds = strings(c.str("notification_ids")),
            archivedAt = c.longOrNull("archived_at"),
            createdAt = c.long("created_at"),
            deletedAt = c.longOrNull("deleted_at"),
        )

        private fun ints(json: String): List<Int> = runCatching {
            val a = JSONArray(json)
            (0 until a.length()).map { a.getInt(it) }
        }.getOrDefault(emptyList())

        private fun strings(json: String): List<String> = runCatching {
            val a = JSONArray(json)
            (0 until a.length()).map { a.getString(it) }
        }.getOrDefault(emptyList())
    }
}

data class Habit(
    val id: String,
    val title: String,
    val emoji: String,
    val kind: String,
    val target: Int,
    val days: Int,
    val startDate: DateStr,
    val position: String,
    val archivedAt: Long?,
    val deletedAt: Long?,
) {
    val schedule get() = Schedule(kind, target, days, startDate)

    companion object {
        fun from(c: Cursor) = Habit(
            c.str("id"), c.str("title"), c.str("emoji"), c.str("kind"), c.int("target"), c.int("days"),
            c.str("start_date"), c.str("position"), c.longOrNull("archived_at"), c.longOrNull("deleted_at"),
        )
    }
}
