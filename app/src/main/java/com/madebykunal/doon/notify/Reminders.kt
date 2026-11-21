package com.madebykunal.doon.notify

import android.Manifest
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.madebykunal.doon.DoonApp
import com.madebykunal.doon.MainActivity
import com.madebykunal.doon.R
import com.madebykunal.doon.data.Countdown
import com.madebykunal.doon.data.CountdownRepo
import com.madebykunal.doon.data.Home
import com.madebykunal.doon.data.SeriesRepo
import com.madebykunal.doon.data.TodoRepo
import com.madebykunal.doon.lib.CountdownMath
import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.Markdown
import com.madebykunal.doon.lib.parseTime
import com.madebykunal.doon.state.DialogAction
import com.madebykunal.doon.state.Dialogs
import com.madebykunal.doon.state.Prefs

data class PlannedAlert(val offset: Int, val at: Long)

object Reminders {
    const val ACTION_TODO = "todo"
    const val ACTION_COUNTDOWN = "countdown"
    const val ACTION_DIGEST = "digest"
    const val ACTION_MIDNIGHT = "midnight"
    private const val CH_TASKS = "tasks"
    private const val CH_COUNTDOWNS = "countdowns"
    private const val CH_DIGEST = "digest"
    private const val DIGEST_NOTIFICATION = 1
    private const val DEFAULT_ALERT_HOUR = 9

    private val ctx: Context get() = DoonApp.context
    private val alarms get() = ctx.getSystemService(AlarmManager::class.java)
    private val notifications get() = ctx.getSystemService(NotificationManager::class.java)
    private var offeredExact = false
    var requestPermission: (() -> Unit)? = null

    fun canExact(): Boolean = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()

    fun canFullScreen(): Boolean = Build.VERSION.SDK_INT < 34 || notifications.canUseFullScreenIntent()

    fun canOverlay(): Boolean = Settings.canDrawOverlays(ctx)

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun askPermission() {
        if (!hasPermission()) requestPermission?.invoke()
    }

    private fun ensureChannels() {
        val sound = { raw: Int -> Uri.parse("android.resource://${ctx.packageName}/$raw") }
        val audio = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        fun channel(id: String, name: String, importance: Int, raw: Int) = NotificationChannel(id, name, importance).apply { setSound(sound(raw), audio) }
        notifications.createNotificationChannels(
            listOf(
                channel(CH_TASKS, "To-do reminders", NotificationManager.IMPORTANCE_HIGH, R.raw.tasks_notification),
                channel(CH_COUNTDOWNS, "Countdowns", NotificationManager.IMPORTANCE_HIGH, R.raw.countdown_notification),
                channel(CH_DIGEST, "Morning digest", NotificationManager.IMPORTANCE_DEFAULT, R.raw.tasks_notification),
            ),
        )
    }

    private fun alarmIntent(action: String, key: String, flags: Int = PendingIntent.FLAG_UPDATE_CURRENT): PendingIntent? =
        PendingIntent.getBroadcast(
            ctx, 0,
            Intent(ctx, AlarmReceiver::class.java).setAction(action).setData(Uri.parse("doon-alarm://$action/$key")).putExtra("key", key),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun cancelAlarm(action: String, key: String) {
        alarmIntent(action, key, PendingIntent.FLAG_NO_CREATE)?.let {
            alarms.cancel(it)
            it.cancel()
        }
    }

    private fun schedule(action: String, key: String, at: Long, clock: Boolean = false, exactWanted: Boolean = true) {
        val pi = alarmIntent(action, key)!!
        when {
            exactWanted && canExact() && clock -> alarms.setAlarmClock(AlarmManager.AlarmClockInfo(at, openApp("alarm", key)), pi)
            exactWanted && canExact() -> alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else -> alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun openApp(kind: String, id: String): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse("doon-open://$kind/$id"))
            .putExtra("open", "$kind:$id").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun syncTodo(id: String, ask: Boolean = false) {
        val todo = TodoRepo.get(id) ?: return
        if (todo.notificationId != null) {
            cancelAlarm(ACTION_TODO, id)
            TodoRepo.update(id, "notification_id" to null)
        }
        val remindAt = todo.remindAt
        if (todo.deletedAt != null || todo.completed || remindAt == null || remindAt <= System.currentTimeMillis()) return
        schedule(ACTION_TODO, id, remindAt, clock = true)
        TodoRepo.update(id, "notification_id" to id)
        if (ask) {
            askPermission()
            offerExactAccess()
        }
    }

    fun cancelTodo(id: String) {
        val todo = TodoRepo.get(id) ?: return
        if (todo.notificationId == null) return
        TodoRepo.update(id, "notification_id" to null)
        cancelAlarm(ACTION_TODO, id)
    }

    fun setTodoReminder(id: String, remindAt: Long?, ask: Boolean = false) {
        TodoRepo.update(id, "remind_at" to remindAt)
        syncTodo(id, ask)
    }

    fun planCountdownAlerts(item: Countdown, today: DateStr, now: Long): List<PlannedAlert> {
        if (item.kind == "since" || item.archivedAt != null) return emptyList()
        val target = CountdownMath.nextOccurrence(item.dates, today)
        return item.notify.map { offset ->
            val (h, m) = parseTime(if (offset == 0) item.targetTime else null, DEFAULT_ALERT_HOUR)
            PlannedAlert(offset, Dates.toMillis(Dates.addDays(target, -offset), h, m))
        }.filter { it.at > now }.sortedBy { it.at }
    }

    fun syncCountdown(id: String, ask: Boolean = false) {
        val item = CountdownRepo.get(id) ?: return
        item.notificationIds.forEach { cancelAlarm(ACTION_COUNTDOWN, it) }
        val plan = if (item.deletedAt != null) emptyList() else planCountdownAlerts(item, Dates.today(), System.currentTimeMillis())
        val keys = plan.map { "${item.id}:${it.offset}" }
        plan.forEachIndexed { i, alert -> schedule(ACTION_COUNTDOWN, keys[i], alert.at) }
        if (keys != item.notificationIds) CountdownRepo.setNotificationIds(id, keys)
        if (ask && plan.isNotEmpty()) {
            askPermission()
            offerExactAccess()
        }
    }

    fun syncDigest(ask: Boolean = false) {
        cancelAlarm(ACTION_DIGEST, "next")
        if (!Prefs.digestEnabled) return
        val (h, m) = parseTime(Prefs.digestTime, 8)
        val now = System.currentTimeMillis()
        var at = Dates.toMillis(Dates.today(), h, m)
        if (at <= now) at = Dates.toMillis(Dates.addDays(Dates.today(), 1), h, m)
        schedule(ACTION_DIGEST, "next", at, exactWanted = false)
        if (ask) askPermission()
    }

    fun scheduleMidnight() {
        val at = System.currentTimeMillis() + Dates.msUntilNextMidnight() + 1000
        schedule(ACTION_MIDNIGHT, "next", at, exactWanted = false)
    }

    fun resyncAll() {
        TodoRepo.pendingReminderIds(System.currentTimeMillis()).forEach { syncTodo(it) }
        CountdownRepo.all().filter { it.notify.isNotEmpty() || it.notificationIds.isNotEmpty() }.forEach { syncCountdown(it.id) }
        syncDigest()
        scheduleMidnight()
    }

    private fun offerExactAccess() {
        if (canExact() || offeredExact || Build.VERSION.SDK_INT < 31) return
        offeredExact = true
        Dialogs.show(
            "Ring on the minute?",
            "Android can hold reminders back by a few minutes. Let Doon set alarms so they ring on time.",
            listOf(DialogAction("Not now", "cancel"), DialogAction("Allow", "primary") { openExactAlarmSettings() }),
        )
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < 31) return
        runCatching {
            ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun openFullScreenSettings() {
        val intent = if (Build.VERSION.SDK_INT >= 34) Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}"))
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
        runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun requestOverlay() {
        if (Build.VERSION.SDK_INT < 33) {
            openOverlaySettings()
            return
        }
        Dialogs.show(
            "Allow display over other apps",
            "Android blocks this for apps installed outside the Play Store. Open App info, tap ⋮ at the top right and choose Allow restricted settings. Then come back and open the setting.",
            listOf(
                DialogAction("App info") { openAppInfo() },
                DialogAction("Open setting", "primary") { openOverlaySettings() },
                DialogAction("Not now", "cancel"),
            ),
        )
    }

    private fun openAppInfo() {
        runCatching {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun openOverlaySettings() {
        runCatching {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun dismiss(notificationId: Int) = notifications.cancel(notificationId)

    fun notificationIdFor(key: String) = key.hashCode() and 0x7FFFFFFF or 0x10

    private fun alertIntent(kind: String, id: String, nid: Int): Intent =
        Intent(ctx, AlertActivity::class.java).setData(Uri.parse("doon-alert://$kind/$id"))
            .putExtra("kind", kind).putExtra("id", id).putExtra("nid", nid)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    private fun post(channel: String, nid: Int, title: String, body: String, open: PendingIntent, alert: Intent?) {
        ensureChannels()
        if (!hasPermission()) return
        val builder = Notification.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setCategory(if (alert != null) Notification.CATEGORY_ALARM else Notification.CATEGORY_REMINDER)
        if (alert != null) {
            builder.setFullScreenIntent(PendingIntent.getActivity(ctx, 0, alert, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE), true)
        }
        notifications.notify(nid, builder.build())
        if (alert != null && (DoonApp.foreground || inUseElsewhere())) runCatching { ctx.startActivity(alert) }
    }

    private fun inUseElsewhere(): Boolean {
        if (!canOverlay()) return false
        val awake = ctx.getSystemService(PowerManager::class.java)?.isInteractive == true
        val locked = ctx.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked != false
        if (!awake || locked) return false
        val state = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(state)
        return state.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    fun fire(action: String, key: String) {
        when (action) {
            ACTION_TODO -> fireTodo(key)
            ACTION_COUNTDOWN -> fireCountdown(key)
            ACTION_DIGEST -> fireDigest()
            ACTION_MIDNIGHT -> DoonApp.dayChanged()
        }
    }

    private fun fireTodo(id: String) {
        val todo = TodoRepo.get(id) ?: return
        TodoRepo.update(id, "notification_id" to null)
        if (todo.deletedAt != null || todo.completed || todo.remindAt == null) return
        val nid = notificationIdFor(id)
        val alert = if (Prefs.fullScreenAlerts) alertIntent("todo", id, nid) else null
        post(CH_TASKS, nid, "Reminder", Markdown.plainText(todo.text), openApp("todo", id), alert)
    }

    private fun fireCountdown(key: String) {
        val id = key.substringBeforeLast(":")
        val offset = key.substringAfterLast(":").toIntOrNull() ?: 0
        val item = CountdownRepo.get(id) ?: return
        if (item.deletedAt == null && item.archivedAt == null) {
            val body = when (offset) {
                0 -> "Today"
                1 -> "Tomorrow"
                7 -> "In a week"
                else -> "In $offset days"
            }
            val nid = notificationIdFor(key)
            val alert = if (Prefs.fullScreenAlerts) alertIntent("countdown", id, nid) else null
            post(CH_COUNTDOWNS, nid, "${item.emoji} ${item.title}", body, openApp("countdown", id), alert)
        }
        syncCountdown(id)
    }

    private fun fireDigest() {
        val today = Dates.today()
        SeriesRepo.materialize(today, today)
        val open = TodoRepo.countOpenOn(today)
        val upcoming = CountdownMath.sort(
            CountdownRepo.all().filter { it.archivedAt == null && it.kind != "since" && !CountdownMath.isPast(it.dates, today) },
            today,
        ) { it.dates }.firstOrNull()
        val body = digestBody(open, upcoming?.let { it.title to CountdownMath.daysUntil(it.dates, today) })
        post(CH_DIGEST, DIGEST_NOTIFICATION, "Good morning", body, openApp("day", today), null)
        syncDigest()
    }

    fun digestBody(openCount: Int, next: Pair<String, Int>?): String {
        val todos = if (openCount == 0) "Nothing planned today" else "$openCount ${if (openCount == 1) "to-do" else "to-dos"} today"
        if (next == null) return todos
        val (title, days) = next
        return if (days == 0) "$todos · $title today" else "$todos · ${if (days == 1) "1 day to" else "$days days to"} $title"
    }

    fun homeOf(todoId: String): Home? = TodoRepo.get(todoId)?.let { t -> t.date?.let { Home.Day(it) } ?: t.listId?.let { Home.InList(it) } }

    val mainComponent get() = ComponentName(ctx, MainActivity::class.java)
}
