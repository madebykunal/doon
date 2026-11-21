package com.madebykunal.doon.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.madebykunal.doon.DoonApp

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!DoonApp.ready) return
        val action = intent.action ?: return
        val key = intent.getStringExtra("key") ?: return
        Reminders.fire(action, key)
    }
}

class SystemReceiver : BroadcastReceiver() {
    private val handled = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (!DoonApp.ready || intent.action !in handled) return
        Reminders.resyncAll()
        DoonApp.dayChanged()
    }
}
