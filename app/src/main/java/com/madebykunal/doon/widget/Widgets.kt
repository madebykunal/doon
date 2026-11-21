package com.madebykunal.doon.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.madebykunal.doon.DoonApp
import com.madebykunal.doon.MainActivity
import com.madebykunal.doon.R
import com.madebykunal.doon.data.CountdownRepo
import com.madebykunal.doon.data.Home
import com.madebykunal.doon.data.SeriesRepo
import com.madebykunal.doon.data.TodoRepo
import com.madebykunal.doon.lib.ColorMath
import com.madebykunal.doon.lib.CountdownMath
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.Markdown
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.state.TodoActions
import com.madebykunal.doon.ui.theme.accentHex

private const val CARD_LIGHT = "#FFFFFF"
private const val CARD_DARK = "#292929"
private const val MAX_ROWS = 5

private data class Row(val id: String, val emoji: String, val title: String, val value: String, val unit: String)

object Widgets {
    private val handler = Handler(Looper.getMainLooper())
    private val pending = Runnable { runCatching { updateAll(DoonApp.context) } }

    fun requestSync() {
        handler.removeCallbacks(pending)
        handler.postDelayed(pending, 300)
    }

    fun updateAll(context: Context) {
        if (!DoonApp.ready) return
        val manager = AppWidgetManager.getInstance(context)
        val single = manager.getAppWidgetIds(ComponentName(context, CountdownSingleWidget::class.java))
        val list = manager.getAppWidgetIds(ComponentName(context, CountdownListWidget::class.java))
        val todos = manager.getAppWidgetIds(ComponentName(context, TodayTodosWidget::class.java))
        if (single.isEmpty() && list.isEmpty() && todos.isEmpty()) return
        val rows = if (single.isNotEmpty() || list.isNotEmpty()) countdownRows() else emptyList()
        single.forEach { manager.updateAppWidget(it, singleViews(context, rows.firstOrNull())) }
        list.forEach { manager.updateAppWidget(it, listViews(context, rows, heightOf(manager, it))) }
        if (todos.isNotEmpty()) {
            val today = Dates.today()
            SeriesRepo.materialize(today, today)
            val open = TodoRepo.items(Home.Day(today)).filter { !it.isHeading && !it.completed }
            todos.forEach { manager.updateAppWidget(it, todoViews(context, open.map { t -> t.id to Markdown.plainText(t.text) }, heightOf(manager, it))) }
        }
    }

    private fun heightOf(manager: AppWidgetManager, id: Int): Int =
        manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160).takeIf { it > 0 } ?: 160

    private fun countdownRows(): List<Row> {
        val today = Dates.today()
        val live = CountdownRepo.all().filter {
            it.showInWidget && it.archivedAt == null && !(it.kind == "until" && it.targetDate < today)
        }
        return CountdownMath.sort(live, today) { it.dates }.map { item ->
            val since = item.kind == "since"
            val target = if (since) item.targetDate else CountdownMath.nextOccurrence(item.dates, today)
            val days = if (since) Dates.diffDays(item.targetDate, today) + 1 else Dates.diffDays(today, target)
            var value = kotlin.math.abs(days).toString()
            var unit = if (since) (if (days == 1) "day" else "days") else "days left"
            if (!since && days == 0) { value = "Today"; unit = "" }
            else if (!since && days == 1) { value = "Tomorrow"; unit = "" }
            Row(item.id, item.emoji, CountdownMath.titleWithAge(item.dates, item.title, today), value, unit)
        }
    }

    private class Palette(val accentLight: Int, val accentDark: Int, val tintLight: Int, val tintDark: Int, val cardLight: Int, val cardDark: Int)

    private fun palette(cardTint: Boolean): Palette {
        val accent = accentHex()
        val cardLight = if (cardTint) ColorMath.mix(CARD_LIGHT, accent, 0.06) else CARD_LIGHT
        val cardDark = if (cardTint) ColorMath.mix(CARD_DARK, accent, 0.08) else CARD_DARK
        return Palette(
            ColorMath.toArgb(ColorMath.ensureContrast(accent, CARD_LIGHT, 4.5)),
            ColorMath.toArgb(ColorMath.ensureContrast(accent, CARD_DARK, 4.5)),
            ColorMath.toArgb(ColorMath.mix(CARD_LIGHT, accent, 0.16)),
            ColorMath.toArgb(ColorMath.mix(CARD_DARK, accent, 0.28)),
            ColorMath.toArgb(cardLight),
            ColorMath.toArgb(cardDark),
        )
    }

    private fun night(context: Context) =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private fun RemoteViews.color(context: Context, id: Int, method: String, light: Int, dark: Int) {
        if (Build.VERSION.SDK_INT >= 31) setColorInt(id, method, light, dark)
        else setInt(id, method, if (night(context)) dark else light)
    }

    private fun open(context: Context, target: String): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse("doon-open://widget/$target"))
            .putExtra("open", target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun singleViews(context: Context, row: Row?): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_countdown_single)
        val p = palette(false)
        v.color(context, R.id.bg, "setColorFilter", p.cardLight, p.cardDark)
        v.setOnClickPendingIntent(R.id.root, open(context, if (row != null) "countdown:${row.id}" else "countdowns"))
        if (row == null) {
            v.setViewVisibility(R.id.content, View.GONE)
            v.setViewVisibility(R.id.empty, View.VISIBLE)
            return v
        }
        v.setViewVisibility(R.id.content, View.VISIBLE)
        v.setViewVisibility(R.id.empty, View.GONE)
        v.color(context, R.id.emoji_bg, "setColorFilter", p.tintLight, p.tintDark)
        v.setTextViewText(R.id.emoji, row.emoji)
        v.setTextViewText(R.id.title, row.title)
        v.setTextViewText(R.id.value, row.value)
        v.setTextViewTextSize(R.id.value, TypedValue.COMPLEX_UNIT_SP, if (row.unit.isNotEmpty()) 50f else 26f)
        v.color(context, R.id.value, "setTextColor", p.accentLight, p.accentDark)
        v.setTextViewText(R.id.unit, row.unit.replaceFirstChar { it.uppercase() })
        v.setViewVisibility(R.id.unit, if (row.unit.isNotEmpty()) View.VISIBLE else View.GONE)
        return v
    }

    private val listRows = listOf(
        intArrayOf(R.id.row0, R.id.emoji_bg0, R.id.emoji0, R.id.title0, R.id.value0),
        intArrayOf(R.id.row1, R.id.emoji_bg1, R.id.emoji1, R.id.title1, R.id.value1),
        intArrayOf(R.id.row2, R.id.emoji_bg2, R.id.emoji2, R.id.title2, R.id.value2),
        intArrayOf(R.id.row3, R.id.emoji_bg3, R.id.emoji3, R.id.title3, R.id.value3),
        intArrayOf(R.id.row4, R.id.emoji_bg4, R.id.emoji4, R.id.title4, R.id.value4),
    )

    private fun listViews(context: Context, rows: List<Row>, height: Int): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_countdown_list)
        val p = palette(false)
        v.color(context, R.id.bg, "setColorFilter", p.cardLight, p.cardDark)
        v.setOnClickPendingIntent(R.id.root, open(context, "countdowns"))
        val fit = minOf(MAX_ROWS, maxOf(2, (height - 20) / 44))
        val shown = rows.take(fit)
        v.setViewVisibility(R.id.empty, if (shown.isEmpty()) View.VISIBLE else View.GONE)
        listRows.forEachIndexed { i, ids ->
            val row = shown.getOrNull(i)
            v.setViewVisibility(ids[0], if (row != null) View.VISIBLE else View.GONE)
            if (row == null) return@forEachIndexed
            v.color(context, ids[1], "setColorFilter", p.tintLight, p.tintDark)
            v.setTextViewText(ids[2], row.emoji)
            v.setTextViewText(ids[3], row.title)
            v.setTextViewText(ids[4], row.value)
            v.setTextViewTextSize(ids[4], TypedValue.COMPLEX_UNIT_SP, if (row.unit.isNotEmpty()) 20f else 16f)
            v.color(context, ids[4], "setTextColor", p.accentLight, p.accentDark)
            v.setOnClickPendingIntent(ids[0], open(context, "countdown:${row.id}"))
        }
        return v
    }

    private val todoRows = listOf(
        intArrayOf(R.id.row0, R.id.line0, R.id.tick0, R.id.ring0, R.id.text0),
        intArrayOf(R.id.row1, R.id.line1, R.id.tick1, R.id.ring1, R.id.text1),
        intArrayOf(R.id.row2, R.id.line2, R.id.tick2, R.id.ring2, R.id.text2),
        intArrayOf(R.id.row3, R.id.line3, R.id.tick3, R.id.ring3, R.id.text3),
        intArrayOf(R.id.row4, R.id.line4, R.id.tick4, R.id.ring4, R.id.text4),
    )

    private fun todoViews(context: Context, open: List<Pair<String, String>>, height: Int): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_todos)
        val p = palette(true)
        val lineLight = 0x14000000 or 0x17171A
        val lineDark = 0x1AFFFFFF
        v.color(context, R.id.bg, "setColorFilter", p.cardLight, p.cardDark)
        v.setOnClickPendingIntent(R.id.root, open(context, "today"))
        v.setOnClickPendingIntent(R.id.add, open(context, "add"))
        v.color(context, R.id.add_bg, "setColorFilter", p.accentLight, p.accentDark)
        v.color(context, R.id.add_plus, "setTextColor", 0xFFFFFFFF.toInt(), 0xFF17171A.toInt())
        v.color(context, R.id.label, "setTextColor", p.accentLight, p.accentDark)
        v.setTextViewText(R.id.count, open.size.toString())
        val fit = minOf(MAX_ROWS, maxOf(3, (height - 60) / 36))
        val rows = if (Prefs.widgetShowsTodos) open.take(fit) else emptyList()
        v.setViewVisibility(R.id.empty, if (rows.isEmpty()) View.VISIBLE else View.GONE)
        v.setTextViewText(R.id.empty, if (open.isNotEmpty()) "Open Doon to see them" else "Nothing planned today")
        todoRows.forEachIndexed { i, ids ->
            val row = rows.getOrNull(i)
            v.setViewVisibility(ids[0], if (row != null) View.VISIBLE else View.GONE)
            v.setViewVisibility(ids[1], if (row != null && i > 0) View.VISIBLE else View.GONE)
            if (row == null) return@forEachIndexed
            v.color(context, ids[1], "setColorFilter", lineLight, lineDark)
            v.color(context, ids[3], "setColorFilter", 0xFF6E6C67.toInt(), 0xFFA3A3A3.toInt())
            v.setTextViewText(ids[4], row.second)
            v.setOnClickPendingIntent(
                ids[2],
                PendingIntent.getBroadcast(
                    context, 0,
                    Intent(context, WidgetActionReceiver::class.java).setAction(WidgetActionReceiver.TICK)
                        .setData(Uri.parse("doon-tick://${row.first}")).putExtra("id", row.first),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
        return v
    }
}

class WidgetActionReceiver : BroadcastReceiver() {
    companion object {
        const val TICK = "com.madebykunal.doon.TICK"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!DoonApp.ready || intent.action != TICK) return
        val id = intent.getStringExtra("id") ?: return
        TodoActions.completeFromWidget(id)
        Widgets.updateAll(context)
    }
}

open class DoonWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Widgets.updateAll(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        Widgets.updateAll(context)
    }
}

class CountdownSingleWidget : DoonWidget()
class CountdownListWidget : DoonWidget()
class TodayTodosWidget : DoonWidget()
