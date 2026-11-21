package com.madebykunal.doon.data

import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.DayLog
import com.madebykunal.doon.lib.FractionalIndex
import org.json.JSONArray

object ListRepo {
    fun groups(): List<ListGroup> = Db.query("SELECT * FROM list_groups WHERE deleted_at IS NULL ORDER BY position", emptyList(), ListGroup::from)

    fun lists(): List<TodoList> = Db.query("SELECT * FROM lists WHERE deleted_at IS NULL ORDER BY position", emptyList(), TodoList::from)

    fun get(id: String): TodoList? = Db.one("SELECT * FROM lists WHERE id = ?", listOf(id), TodoList::from)

    fun isLive(id: String): Boolean = Db.one("SELECT deleted_at IS NULL FROM lists WHERE id = ?", listOf(id)) { it.getInt(0) == 1 } ?: false

    fun counts(): Map<String, Int> = Db.query(
        "SELECT list_id, count(*) FROM todos WHERE list_id IS NOT NULL AND completed_at IS NULL AND is_heading = 0 AND deleted_at IS NULL GROUP BY list_id",
    ) { it.getString(0) to it.getInt(1) }.toMap()

    private fun lastGroupPosition() = Db.one("SELECT position FROM list_groups WHERE deleted_at IS NULL ORDER BY position DESC") { it.getString(0) }

    private fun lastListPosition(groupId: String) =
        Db.one("SELECT position FROM lists WHERE group_id = ? AND deleted_at IS NULL ORDER BY position DESC", listOf(groupId)) { it.getString(0) }

    fun createGroup(name: String): String {
        val id = Db.uuid()
        val now = System.currentTimeMillis()
        Db.insert("list_groups", values("id" to id, "name" to name, "position" to FractionalIndex.between(lastGroupPosition(), null), "created_at" to now, "updated_at" to now))
        Live.changed(T.GROUPS)
        return id
    }

    fun updateGroup(id: String, vararg patch: Pair<String, Any?>) {
        Db.update("list_groups", values(*patch, "updated_at" to System.currentTimeMillis()), "id = ?", listOf(id))
        Live.changed(T.GROUPS)
    }

    fun deleteGroup(id: String) {
        val listIds = Db.query("SELECT id FROM lists WHERE group_id = ? AND deleted_at IS NULL", listOf(id)) { it.getString(0) }
        Db.tx {
            val now = System.currentTimeMillis()
            val gone = values("deleted_at" to now, "updated_at" to now)
            Db.update("list_groups", gone, "id = ?", listOf(id))
            if (listIds.isNotEmpty()) {
                val ph = Db.placeholders(listIds.size)
                Db.update("lists", gone, "id IN ($ph)", listIds)
                Db.update("todos", gone, "list_id IN ($ph)", listIds)
            }
        }
        Live.changed(T.GROUPS, T.LISTS, T.TODOS)
    }

    fun createList(name: String, groupId: String, emoji: String? = null): String {
        val id = Db.uuid()
        val now = System.currentTimeMillis()
        Db.insert(
            "lists",
            values("id" to id, "name" to name, "emoji" to emoji, "group_id" to groupId, "position" to FractionalIndex.between(lastListPosition(groupId), null), "created_at" to now, "updated_at" to now),
        )
        Live.changed(T.LISTS)
        return id
    }

    fun updateList(id: String, vararg patch: Pair<String, Any?>) {
        Db.update("lists", values(*patch, "updated_at" to System.currentTimeMillis()), "id = ?", listOf(id))
        Live.changed(T.LISTS)
    }

    fun reorderGroup(id: String, beforeKey: String?, afterKey: String?) =
        updateGroup(id, "position" to FractionalIndex.between(beforeKey, afterKey))

    fun reorderList(id: String, beforeKey: String?, afterKey: String?) =
        updateList(id, "position" to FractionalIndex.between(beforeKey, afterKey))

    fun deleteList(id: String) {
        Db.tx {
            val now = System.currentTimeMillis()
            val gone = values("deleted_at" to now, "updated_at" to now)
            Db.update("lists", gone, "id = ?", listOf(id))
            Db.update("todos", gone, "list_id = ?", listOf(id))
        }
        Live.changed(T.LISTS, T.TODOS)
    }

    fun seedDefaults() {
        if (Db.one("SELECT id FROM list_groups") { it.getString(0) } != null) return
        Db.tx {
            createList("Brain dump", createGroup("My lists"), "🧠")
            val planning = createGroup("Planning")
            listOf("This week", "Next week", "This month", "Next month", "Someday soon").forEach { createList(it, planning) }
        }
    }
}

object CountdownRepo {
    fun all(): List<Countdown> = Db.query("SELECT * FROM countdowns WHERE deleted_at IS NULL", emptyList(), Countdown::from)

    fun get(id: String): Countdown? = Db.one("SELECT * FROM countdowns WHERE id = ?", listOf(id), Countdown::from)

    fun create(title: String, emoji: String, kind: String, targetDate: DateStr, targetTime: String?, showInWidget: Boolean, notify: List<Int>): String {
        val id = Db.uuid()
        val now = System.currentTimeMillis()
        Db.insert(
            "countdowns",
            values(
                "id" to id, "title" to title, "emoji" to emoji, "color" to "accent", "kind" to kind, "target_date" to targetDate,
                "target_time" to targetTime, "show_in_widget" to showInWidget, "widget_position" to "a0", "show_on_calendar" to false,
                "notify" to JSONArray(notify).toString(), "notification_ids" to "[]", "created_at" to now, "updated_at" to now,
            ),
        )
        Live.changed(T.COUNTDOWNS)
        return id
    }

    fun update(id: String, vararg patch: Pair<String, Any?>) {
        Db.update("countdowns", values(*patch, "updated_at" to System.currentTimeMillis()), "id = ?", listOf(id))
        Live.changed(T.COUNTDOWNS)
    }

    fun setNotificationIds(id: String, ids: List<String>) {
        Db.update("countdowns", values("notification_ids" to JSONArray(ids).toString()), "id = ?", listOf(id))
    }

    fun softDelete(id: String) = update(id, "deleted_at" to System.currentTimeMillis())

    fun restore(id: String) = update(id, "deleted_at" to null)
}

object HabitRepo {
    fun all(): List<Habit> = Db.query("SELECT * FROM habits WHERE deleted_at IS NULL ORDER BY position, id", emptyList(), Habit::from)

    fun get(id: String): Habit? = Db.one("SELECT * FROM habits WHERE id = ?", listOf(id), Habit::from)

    fun logs(habitId: String): List<DayLog> =
        Db.query("SELECT date, count FROM habit_logs WHERE habit_id = ? AND count > 0 ORDER BY date DESC", listOf(habitId)) { DayLog(it.getString(0), it.getInt(1)) }

    fun create(title: String, kind: String, target: Int, days: Int, startDate: DateStr, cue: String?) {
        val last = Db.one("SELECT position FROM habits WHERE deleted_at IS NULL ORDER BY position DESC") { it.getString(0) }
        val now = System.currentTimeMillis()
        Db.insert(
            "habits",
            values(
                "id" to Db.uuid(), "title" to title, "emoji" to "", "kind" to kind, "target" to target, "days" to days, "cue" to cue,
                "start_date" to startDate, "position" to FractionalIndex.between(last, null), "created_at" to now, "updated_at" to now,
            ),
        )
        Live.changed(T.HABITS)
    }

    fun update(id: String, vararg patch: Pair<String, Any?>) {
        Db.update("habits", values(*patch, "updated_at" to System.currentTimeMillis()), "id = ?", listOf(id))
        Live.changed(T.HABITS)
    }

    fun softDelete(id: String) = update(id, "deleted_at" to System.currentTimeMillis())

    fun restore(id: String) = update(id, "deleted_at" to null)

    fun setCount(habitId: String, date: DateStr, count: Int) {
        val now = System.currentTimeMillis()
        val changed = Db.update("habit_logs", values("count" to count, "updated_at" to now), "habit_id = ? AND date = ?", listOf(habitId, date))
        if (changed == 0) Db.insert("habit_logs", values("habit_id" to habitId, "date" to date, "count" to count, "created_at" to now, "updated_at" to now))
        Live.changed(T.HABIT_LOGS)
    }

    fun eraseDeleted(deletedBefore: Long) {
        val gone = Db.query("SELECT id FROM habits WHERE deleted_at < ? AND title != ''", listOf(deletedBefore)) { it.getString(0) }
        if (gone.isEmpty()) return
        val ph = Db.placeholders(gone.size)
        Db.tx {
            Db.update("habits", values("title" to "", "emoji" to "", "cue" to null, "updated_at" to System.currentTimeMillis()), "id IN ($ph)", gone)
            Db.db.delete("habit_logs", "habit_id IN ($ph)", gone.toTypedArray())
        }
        Db.checkpoint()
    }
}
