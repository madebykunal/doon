package com.madebykunal.doon.data

import android.content.ContentValues
import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.FractionalIndex

sealed interface Where {
    data object Top : Where
    data object Bottom : Where
    data class After(val id: String) : Where
}

object TodoRepo {
    private const val LIVE = "deleted_at IS NULL"
    private const val OPEN = "completed_at IS NULL AND is_heading = 0 AND deleted_at IS NULL"

    private fun inHome(home: Home): Pair<String, String> = when (home) {
        is Home.Day -> "date = ?" to home.date
        is Home.InList -> "list_id = ?" to home.listId
    }

    private fun homeValues(home: Home, cv: ContentValues) {
        when (home) {
            is Home.Day -> {
                cv.put("date", home.date)
                cv.putNull("list_id")
            }
            is Home.InList -> {
                cv.putNull("date")
                cv.put("list_id", home.listId)
            }
        }
    }

    fun items(home: Home): List<Todo> {
        val (where, arg) = inHome(home)
        return Db.query("SELECT * FROM todos WHERE $where AND $LIVE ORDER BY position, id", listOf(arg), Todo::from)
    }

    fun openDates(from: DateStr, to: DateStr): Set<String> =
        Db.query("SELECT DISTINCT date FROM todos WHERE date BETWEEN ? AND ? AND $OPEN", listOf(from, to)) { it.getString(0) }.toHashSet()

    fun get(id: String): Todo? = Db.one("SELECT * FROM todos WHERE id = ?", listOf(id), Todo::from)

    fun pendingReminderIds(now: Long): List<String> =
        Db.query("SELECT id FROM todos WHERE remind_at > ? AND completed_at IS NULL AND $LIVE", listOf(now)) { it.getString(0) }

    fun countOpenOn(date: DateStr): Int =
        Db.one("SELECT count(*) FROM todos WHERE date = ? AND $OPEN", listOf(date)) { it.getInt(0) } ?: 0

    fun search(term: String, limit: Int = 50): List<Todo> {
        val like = Db.likeArg(term)
        return Db.query(
            "SELECT * FROM todos WHERE (text LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\') AND $LIVE ORDER BY date DESC LIMIT $limit",
            listOf(like, like),
            Todo::from,
        )
    }

    private fun firstPosition(home: Home): String? {
        val (where, arg) = inHome(home)
        return Db.one("SELECT position FROM todos WHERE $where AND $LIVE ORDER BY position", listOf(arg)) { it.getString(0) }
    }

    fun lastPosition(home: Home): String? {
        val (where, arg) = inHome(home)
        return Db.one("SELECT position FROM todos WHERE $where AND $LIVE ORDER BY position DESC", listOf(arg)) { it.getString(0) }
    }

    private fun positionFor(home: Home, where: Where): String = when (where) {
        Where.Top -> FractionalIndex.between(null, firstPosition(home))
        Where.Bottom -> FractionalIndex.between(lastPosition(home), null)
        is Where.After -> {
            val items = items(home)
            val index = items.indexOfFirst { it.id == where.id }
            val before = items.getOrNull(index)?.position ?: lastPosition(home)
            FractionalIndex.between(before, if (index >= 0) items.getOrNull(index + 1)?.position else null)
        }
    }

    fun create(
        home: Home,
        text: String,
        description: String? = null,
        where: Where = Where.Bottom,
        isHeading: Boolean = false,
        highlight: String? = null,
        seriesId: String? = null,
        seriesDate: DateStr? = null,
    ): Todo {
        val id = Db.uuid()
        val now = System.currentTimeMillis()
        val cv = values(
            "id" to id, "text" to text, "description" to description, "is_heading" to isHeading,
            "position" to positionFor(home, where), "highlight" to highlight, "series_id" to seriesId,
            "series_date" to seriesDate, "created_at" to now, "updated_at" to now,
        )
        homeValues(home, cv)
        Db.insert("todos", cv)
        Live.changed(T.TODOS)
        return get(id)!!
    }

    fun update(id: String, vararg patch: Pair<String, Any?>) {
        val cv = values(*patch)
        cv.put("updated_at", System.currentTimeMillis())
        Db.update("todos", cv, "id = ?", listOf(id))
        Live.changed(T.TODOS)
    }

    fun eraseDeletedText(deletedBefore: Long) {
        val now = System.currentTimeMillis()
        val erased = Db.tx {
            val blank = values("text" to "", "description" to null, "updated_at" to now)
            Db.update("todos", blank, "deleted_at < ? AND text != ''", listOf(deletedBefore.toString())) +
                Db.update("series", blank, "deleted_at < ? AND text != ''", listOf(deletedBefore.toString()))
        }
        if (erased > 0) Db.checkpoint()
    }

    fun setCompleted(ids: List<String>, completedAt: Long?) {
        if (ids.isEmpty()) return
        Db.update(
            "todos",
            values("completed_at" to completedAt, "updated_at" to System.currentTimeMillis()),
            "id IN (${Db.placeholders(ids.size)})",
            ids,
        )
        Live.changed(T.TODOS)
    }

    fun softDelete(id: String) {
        val now = System.currentTimeMillis()
        update(id, "deleted_at" to now)
    }

    fun restore(id: String) = update(id, "deleted_at" to null)

    fun placementOf(todo: Todo) = Placement(todo.date, todo.listId, todo.position)

    fun restorePlacement(id: String, p: Placement) = update(id, "date" to p.date, "list_id" to p.listId, "position" to p.position)

    fun move(id: String, home: Home) {
        val cv = ContentValues()
        homeValues(home, cv)
        cv.put("position", positionFor(home, Where.Bottom))
        cv.put("updated_at", System.currentTimeMillis())
        Db.update("todos", cv, "id = ?", listOf(id))
        Live.changed(T.TODOS)
    }

    fun reorder(id: String, beforeKey: String?, afterKey: String?) =
        update(id, "position" to FractionalIndex.between(beforeKey, afterKey))

    fun rollover(today: DateStr): Int {
        val moved = Db.tx {
            val overdue = Db.query(
                "SELECT * FROM todos WHERE date < ? AND series_id IS NULL AND $OPEN ORDER BY date, position",
                listOf(today),
                Todo::from,
            )
            if (overdue.isEmpty()) return@tx 0
            val keys = FractionalIndex.nBetween(null, firstPosition(Home.Day(today)), overdue.size)
            val now = System.currentTimeMillis()
            overdue.forEachIndexed { i, todo ->
                Db.update(
                    "todos",
                    values("date" to today, "position" to keys[i], "rollover_count" to todo.rolloverCount + 1, "updated_at" to now),
                    "id = ?",
                    listOf(todo.id),
                )
            }
            overdue.size
        }
        if (moved > 0) Live.changed(T.TODOS)
        return moved
    }

    fun moveOpenItems(from: DateStr, to: Home): List<Pair<String, Placement>> {
        val moved = Db.tx {
            val open = Db.query("SELECT * FROM todos WHERE date = ? AND $OPEN ORDER BY position", listOf(from), Todo::from)
            if (open.isEmpty()) return@tx emptyList()
            val keys = FractionalIndex.nBetween(lastPosition(to), null, open.size)
            val now = System.currentTimeMillis()
            open.forEachIndexed { i, todo ->
                val cv = ContentValues()
                homeValues(to, cv)
                cv.put("position", keys[i])
                cv.put("updated_at", now)
                Db.update("todos", cv, "id = ?", listOf(todo.id))
            }
            open.map { it.id to placementOf(it) }
        }
        if (moved.isNotEmpty()) Live.changed(T.TODOS)
        return moved
    }
}
