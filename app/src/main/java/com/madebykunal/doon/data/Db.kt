package com.madebykunal.doon.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.runtime.mutableIntStateOf
import java.io.File
import java.util.UUID

object T {
    const val TODOS = "todos"
    const val LISTS = "lists"
    const val GROUPS = "list_groups"
    const val SERIES = "series"
    const val COUNTDOWNS = "countdowns"
    const val HABITS = "habits"
    const val HABIT_LOGS = "habit_logs"
}

object Live {
    private val versions = HashMap<String, androidx.compose.runtime.MutableIntState>()
    private val listeners = mutableListOf<(Set<String>) -> Unit>()

    private fun state(table: String) = versions.getOrPut(table) { mutableIntStateOf(0) }

    fun version(table: String): Int = state(table).intValue

    fun changed(vararg tables: String) {
        tables.forEach { state(it).intValue++ }
        val set = tables.toSet()
        listeners.forEach { it(set) }
    }

    fun listen(listener: (Set<String>) -> Unit) {
        listeners.add(listener)
    }
}

private const val VERSION = 5

private val SCHEMA_V1 = listOf(
    """CREATE TABLE `countdowns` (`id` text PRIMARY KEY NOT NULL, `title` text NOT NULL, `emoji` text NOT NULL, `color` text NOT NULL, `kind` text NOT NULL, `target_date` text NOT NULL, `target_time` text, `birth_year` integer, `show_in_widget` integer DEFAULT 1 NOT NULL, `widget_position` text NOT NULL, `show_on_calendar` integer DEFAULT 0 NOT NULL, `notify` text NOT NULL, `notification_ids` text NOT NULL, `archived_at` integer, `created_at` integer NOT NULL, `updated_at` integer NOT NULL, `deleted_at` integer)""",
    """CREATE TABLE `list_groups` (`id` text PRIMARY KEY NOT NULL, `name` text NOT NULL, `position` text NOT NULL, `collapsed` integer DEFAULT 0 NOT NULL, `created_at` integer NOT NULL, `updated_at` integer NOT NULL, `deleted_at` integer)""",
    """CREATE TABLE `lists` (`id` text PRIMARY KEY NOT NULL, `name` text NOT NULL, `emoji` text, `group_id` text NOT NULL, `position` text NOT NULL, `created_at` integer NOT NULL, `updated_at` integer NOT NULL, `deleted_at` integer, FOREIGN KEY (`group_id`) REFERENCES `list_groups`(`id`) ON UPDATE no action ON DELETE no action)""",
    "CREATE INDEX `lists_group_position` ON `lists` (`group_id`,`position`)",
    """CREATE TABLE `series` (`id` text PRIMARY KEY NOT NULL, `text` text NOT NULL, `rule` text NOT NULL, `start_date` text NOT NULL, `end_date` text, `highlight` text, `created_at` integer NOT NULL, `updated_at` integer NOT NULL, `deleted_at` integer)""",
    """CREATE TABLE `series_exceptions` (`series_id` text NOT NULL, `date` text NOT NULL, `kind` text NOT NULL, PRIMARY KEY(`series_id`, `date`))""",
    """CREATE TABLE `todos` (`id` text PRIMARY KEY NOT NULL, `text` text NOT NULL, `is_heading` integer DEFAULT 0 NOT NULL, `list_id` text, `date` text, `position` text NOT NULL, `highlight` text, `completed_at` integer, `series_id` text, `series_date` text, `rollover_count` integer DEFAULT 0 NOT NULL, `remind_at` integer, `notification_id` text, `collapsed` integer DEFAULT 0 NOT NULL, `created_at` integer NOT NULL, `updated_at` integer NOT NULL, `deleted_at` integer, FOREIGN KEY (`list_id`) REFERENCES `lists`(`id`) ON UPDATE no action ON DELETE no action, CONSTRAINT "todos_one_home" CHECK(("todos"."date" IS NULL) <> ("todos"."list_id" IS NULL)))""",
    "CREATE INDEX `todos_date_position` ON `todos` (`date`,`position`)",
    "CREATE INDEX `todos_list_position` ON `todos` (`list_id`,`position`)",
    "CREATE UNIQUE INDEX `todos_series_instance` ON `todos` (`series_id`,`series_date`)",
)

private val SCHEMA_V2 = listOf(
    """CREATE TABLE `habit_logs` (`habit_id` text NOT NULL, `date` text NOT NULL, `count` integer NOT NULL, `created_at` integer NOT NULL, `updated_at` integer NOT NULL, PRIMARY KEY(`habit_id`, `date`))""",
    """CREATE TABLE `habits` (`id` text PRIMARY KEY NOT NULL, `title` text NOT NULL, `emoji` text NOT NULL, `kind` text NOT NULL, `target` integer DEFAULT 1 NOT NULL, `days` integer DEFAULT 127 NOT NULL, `start_date` text NOT NULL, `position` text NOT NULL, `archived_at` integer, `created_at` integer NOT NULL, `updated_at` integer NOT NULL, `deleted_at` integer)""",
)

private val SCHEMA_V3 = listOf(
    "ALTER TABLE `series` ADD `description` text",
    "ALTER TABLE `todos` ADD `description` text",
)

private val SCHEMA_V4 = listOf(
    "UPDATE `todos` SET `is_heading` = 0, `collapsed` = 0 WHERE `is_heading` = 1",
)

private val SCHEMA_V5 = listOf(
    "ALTER TABLE `habits` ADD `cue` text",
)

private val MIGRATIONS = listOf(SCHEMA_V1, SCHEMA_V2, SCHEMA_V3, SCHEMA_V4, SCHEMA_V5)

private class Helper(context: Context) :
    SQLiteOpenHelper(context, Db.NAME, null, VERSION, { }) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        migrate(db, existingLevel(db))
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        migrate(db, oldVersion)
    }

    override fun onOpen(db: SQLiteDatabase) {
        db.pragma("PRAGMA synchronous = NORMAL")
        db.pragma("PRAGMA secure_delete = ON")
    }

    private fun existingLevel(db: SQLiteDatabase): Int {
        fun hasTable(name: String) = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(name)).use { it.moveToFirst() }
        if (!hasTable("todos")) return 0
        if (!hasTable("habits")) return 1
        val hasDescription = db.rawQuery("PRAGMA table_info(`todos`)", null).use { c ->
            var found = false
            while (c.moveToNext()) if (c.getString(c.getColumnIndexOrThrow("name")) == "description") found = true
            found
        }
        return if (hasDescription) 3 else 2
    }

    private fun migrate(db: SQLiteDatabase, from: Int) {
        for (level in from until VERSION) MIGRATIONS[level].forEach(db::execSQL)
    }
}

object Db {
    const val NAME = "doon.db"
    private var helper: SQLiteOpenHelper? = null
    var openError: Throwable? = null
        private set

    val db: SQLiteDatabase get() = helper!!.writableDatabase

    fun open(context: Context) {
        adoptLegacyFile(context)
        try {
            val h = Helper(context)
            h.setWriteAheadLoggingEnabled(true)
            h.writableDatabase
            helper = h
        } catch (e: Throwable) {
            openError = e
        }
    }

    private fun adoptLegacyFile(context: Context) {
        val target = context.getDatabasePath(NAME)
        val legacy = File(context.filesDir, "SQLite/$NAME")
        if (target.exists() || !legacy.exists()) return
        target.parentFile?.mkdirs()
        listOf("", "-wal", "-shm").forEach { suffix ->
            val from = File(legacy.path + suffix)
            if (from.exists()) from.copyTo(File(target.path + suffix), overwrite = false)
        }
    }

    fun rawFile(context: Context): File = context.getDatabasePath(NAME)

    fun uuid(): String = UUID.randomUUID().toString()

    inline fun <T> tx(block: () -> T): T {
        val d = db
        d.beginTransaction()
        try {
            val result = block()
            d.setTransactionSuccessful()
            return result
        } finally {
            d.endTransaction()
        }
    }

    fun <T> query(sql: String, args: List<Any?> = emptyList(), map: (Cursor) -> T): List<T> =
        db.rawQuery(sql, args.map { it?.toString() }.toTypedArray()).use { c ->
            val out = ArrayList<T>(c.count)
            while (c.moveToNext()) out.add(map(c))
            out
        }

    fun <T> one(sql: String, args: List<Any?> = emptyList(), map: (Cursor) -> T): T? = query("$sql LIMIT 1", args, map).firstOrNull()

    fun exec(sql: String, args: List<Any?> = emptyList()) {
        db.execSQL(sql, args.toTypedArray())
    }

    fun update(table: String, values: ContentValues, where: String, args: List<String>): Int =
        db.update(table, values, where, args.toTypedArray())

    fun insert(table: String, values: ContentValues, ignoreConflict: Boolean = false): Long =
        if (ignoreConflict) db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        else db.insertOrThrow(table, null, values)

    fun checkpoint() {
        db.pragma("PRAGMA wal_checkpoint(TRUNCATE)")
    }

    fun placeholders(n: Int): String = List(n) { "?" }.joinToString(",")

    fun likeArg(term: String): String = "%" + term.replace(Regex("[\\\\%_]")) { "\\" + it.value } + "%"
}

private fun SQLiteDatabase.pragma(sql: String) {
    rawQuery(sql, null).use { it.moveToFirst() }
}

fun Cursor.str(name: String): String = getString(getColumnIndexOrThrow(name))
fun Cursor.strOrNull(name: String): String? = getColumnIndexOrThrow(name).let { if (isNull(it)) null else getString(it) }
fun Cursor.long(name: String): Long = getLong(getColumnIndexOrThrow(name))
fun Cursor.longOrNull(name: String): Long? = getColumnIndexOrThrow(name).let { if (isNull(it)) null else getLong(it) }
fun Cursor.int(name: String): Int = getInt(getColumnIndexOrThrow(name))
fun Cursor.intOrNull(name: String): Int? = getColumnIndexOrThrow(name).let { if (isNull(it)) null else getInt(it) }
fun Cursor.bool(name: String): Boolean = getInt(getColumnIndexOrThrow(name)) != 0
fun Cursor.optStr(name: String): String? = getColumnIndex(name).let { if (it < 0 || isNull(it)) null else getString(it) }

fun values(vararg pairs: Pair<String, Any?>): ContentValues = ContentValues().apply {
    pairs.forEach { (k, v) ->
        when (v) {
            null -> putNull(k)
            is String -> put(k, v)
            is Int -> put(k, v)
            is Long -> put(k, v)
            is Boolean -> put(k, if (v) 1 else 0)
            is Double -> put(k, v)
            else -> put(k, v.toString())
        }
    }
}
