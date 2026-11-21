package com.madebykunal.doon.data

import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.FractionalIndex
import com.madebykunal.doon.lib.Recurrence
import com.madebykunal.doon.lib.RepeatRule

object SeriesRepo {
    fun all(): List<Series> =
        Db.query("SELECT * FROM series WHERE deleted_at IS NULL ORDER BY text", emptyList(), Series::from).filterNotNull()

    fun get(id: String): Series? = Db.one("SELECT * FROM series WHERE id = ?", listOf(id), Series::from)

    fun create(text: String, rule: RepeatRule, startDate: DateStr, highlight: String? = null, description: String? = null): Series {
        val id = Db.uuid()
        val now = System.currentTimeMillis()
        Db.insert(
            "series",
            values(
                "id" to id, "text" to text, "description" to description, "rule" to rule.toJson(), "start_date" to startDate,
                "highlight" to highlight, "created_at" to now, "updated_at" to now,
            ),
        )
        Live.changed(T.SERIES)
        return Series(id, text, description, rule, startDate, null, highlight)
    }

    fun materialize(from: DateStr, to: DateStr) {
        val due = all().flatMap { item -> Recurrence.expand(item.rule, item.startDate, item.endDate, from, to).map { item to it } }
        if (due.isEmpty()) return
        val ids = due.map { it.first.id }.distinct()
        val ph = Db.placeholders(ids.size)
        val settled = HashSet<String>()
        Db.query("SELECT series_id, series_date FROM todos WHERE series_id IN ($ph) AND series_date BETWEEN ? AND ?", ids + from + to) {
            "${it.getString(0)}|${it.getString(1)}"
        }.forEach(settled::add)
        Db.query("SELECT series_id, date FROM series_exceptions WHERE series_id IN ($ph) AND date BETWEEN ? AND ?", ids + from + to) {
            "${it.getString(0)}|${it.getString(1)}"
        }.forEach(settled::add)
        val missing = due.filter { (item, date) -> "${item.id}|$date" !in settled }
        if (missing.isEmpty()) return
        Db.tx {
            for ((item, date) in missing) {
                val now = System.currentTimeMillis()
                Db.insert(
                    "todos",
                    values(
                        "id" to Db.uuid(), "text" to item.text, "description" to item.description, "date" to date,
                        "position" to FractionalIndex.between(TodoRepo.lastPosition(Home.Day(date)), null),
                        "highlight" to item.highlight, "series_id" to item.id, "series_date" to date,
                        "created_at" to now, "updated_at" to now,
                    ),
                    ignoreConflict = true,
                )
            }
        }
        Live.changed(T.TODOS)
    }

    private fun setException(seriesId: String, date: DateStr, kind: String) {
        Db.exec("INSERT OR REPLACE INTO series_exceptions (series_id, date, kind) VALUES (?, ?, ?)", listOf(seriesId, date, kind))
    }

    fun detachInstance(seriesId: String, date: DateStr) = setException(seriesId, date, "detached")

    fun skipInstance(todoId: String, seriesId: String, date: DateStr) {
        Db.tx {
            val now = System.currentTimeMillis()
            Db.update("todos", values("deleted_at" to now, "updated_at" to now), "id = ?", listOf(todoId))
            setException(seriesId, date, "skip")
        }
        Live.changed(T.TODOS)
    }

    private fun followingInstanceIds(seriesId: String, date: DateStr): List<String> {
        val excepted = Db.query("SELECT date FROM series_exceptions WHERE series_id = ?", listOf(seriesId)) { it.getString(0) }.toHashSet()
        return Db.query(
            "SELECT id, series_date FROM todos WHERE series_id = ? AND series_date >= ? AND completed_at IS NULL AND deleted_at IS NULL",
            listOf(seriesId, date),
        ) { it.getString(0) to it.getString(1) }
            .filter { it.second !in excepted }
            .map { it.first }
    }

    private fun updateIds(ids: List<String>, vararg patch: Pair<String, Any?>) {
        if (ids.isEmpty()) return
        Db.update("todos", values(*patch), "id IN (${Db.placeholders(ids.size)})", ids)
    }

    fun updateText(seriesId: String, fromDate: DateStr, vararg patch: Pair<String, Any?>) {
        val ids = followingInstanceIds(seriesId, fromDate)
        val change = arrayOf(*patch, "updated_at" to System.currentTimeMillis())
        Db.tx {
            Db.update("series", values(*change), "id = ?", listOf(seriesId))
            updateIds(ids, *change)
        }
        Live.changed(T.SERIES, T.TODOS)
    }

    fun changeRule(seriesId: String, fromDate: DateStr, rule: RepeatRule) {
        val current = get(seriesId) ?: return
        val ids = followingInstanceIds(seriesId, fromDate)
        Db.tx {
            val now = System.currentTimeMillis()
            Db.update("series", values("end_date" to Dates.addDays(fromDate, -1), "updated_at" to now), "id = ?", listOf(seriesId))
            updateIds(ids, "deleted_at" to now, "updated_at" to now)
            Db.insert(
                "series",
                values(
                    "id" to Db.uuid(), "text" to current.text, "description" to current.description, "rule" to rule.toJson(),
                    "start_date" to fromDate, "end_date" to current.endDate, "highlight" to current.highlight,
                    "created_at" to now, "updated_at" to now,
                ),
            )
        }
        Live.changed(T.SERIES, T.TODOS)
    }

    fun delete(seriesId: String, fromDate: DateStr) {
        val ids = followingInstanceIds(seriesId, fromDate)
        Db.tx {
            val now = System.currentTimeMillis()
            Db.update("series", values("deleted_at" to now, "updated_at" to now), "id = ?", listOf(seriesId))
            updateIds(ids, "deleted_at" to now, "updated_at" to now)
        }
        Live.changed(T.SERIES, T.TODOS)
    }
}
