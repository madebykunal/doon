package com.madebykunal.doon.state

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.madebykunal.doon.lib.Dates

object Prefs {
    private lateinit var prefs: SharedPreferences
    private val listeners = mutableListOf<(String) -> Unit>()

    @set:JvmName("assign_themeMode")
    var themeMode by mutableStateOf("system")
        private set
    @set:JvmName("assign_accent")
    var accent by mutableStateOf("lagoon")
        private set
    @set:JvmName("assign_customColor")
    var customColor by mutableStateOf("#3B5BDB")
        private set
    @set:JvmName("assign_hideCompleted")
    var hideCompleted by mutableStateOf(false)
        private set
    @set:JvmName("assign_sinkCompleted")
    var sinkCompleted by mutableStateOf(false)
        private set
    @set:JvmName("assign_rollover")
    var rollover by mutableStateOf(true)
        private set
    var weekStartsOn by mutableStateOf<Int?>(null); private set
    @set:JvmName("assign_celebration")
    var celebration by mutableStateOf(true)
        private set
    @set:JvmName("assign_weekStrip")
    var weekStrip by mutableStateOf(true)
        private set
    @set:JvmName("assign_weekBand")
    var weekBand by mutableStateOf(true)
        private set
    @set:JvmName("assign_digestEnabled")
    var digestEnabled by mutableStateOf(false)
        private set
    @set:JvmName("assign_digestTime")
    var digestTime by mutableStateOf("08:00")
        private set
    @set:JvmName("assign_defaultCountdownAlerts")
    var defaultCountdownAlerts by mutableStateOf(listOf(0))
        private set
    @set:JvmName("assign_widgetShowsTodos")
    var widgetShowsTodos by mutableStateOf(true)
        private set
    @set:JvmName("assign_fullScreenAlerts")
    var fullScreenAlerts by mutableStateOf(false)
        private set

    val weekStart: Int get() = weekStartsOn ?: Dates.localeWeekStart()

    fun load(context: Context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        themeMode = prefs.getString("themeMode", themeMode)!!
        accent = prefs.getString("accent", accent)!!
        customColor = prefs.getString("customColor", customColor)!!
        hideCompleted = prefs.getBoolean("hideCompleted", hideCompleted)
        sinkCompleted = prefs.getBoolean("sinkCompleted", sinkCompleted)
        rollover = prefs.getBoolean("rollover", rollover)
        weekStartsOn = prefs.getInt("weekStartsOn", -1).takeIf { it >= 0 }
        celebration = prefs.getBoolean("celebration", celebration)
        weekStrip = prefs.getBoolean("weekStrip", weekStrip)
        weekBand = prefs.getBoolean("weekBand", weekBand)
        digestEnabled = prefs.getBoolean("digestEnabled", digestEnabled)
        digestTime = prefs.getString("digestTime", digestTime)!!
        defaultCountdownAlerts = prefs.getString("defaultCountdownAlerts", null)
            ?.split(",")?.mapNotNull { it.toIntOrNull() } ?: defaultCountdownAlerts
        widgetShowsTodos = prefs.getBoolean("widgetShowsTodos", widgetShowsTodos)
        fullScreenAlerts = prefs.getBoolean("fullScreenAlerts", fullScreenAlerts)
    }

    fun listen(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    private fun saved(key: String, write: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(write).apply()
        listeners.forEach { it(key) }
    }

    fun setThemeMode(v: String) { themeMode = v; saved("themeMode") { putString("themeMode", v) } }
    fun setAccent(v: String) { accent = v; saved("accent") { putString("accent", v) } }
    fun setCustomColor(v: String) { customColor = v; saved("customColor") { putString("customColor", v) } }
    fun setHideCompleted(v: Boolean) { hideCompleted = v; saved("hideCompleted") { putBoolean("hideCompleted", v) } }
    fun setSinkCompleted(v: Boolean) { sinkCompleted = v; saved("sinkCompleted") { putBoolean("sinkCompleted", v) } }
    fun setRollover(v: Boolean) { rollover = v; saved("rollover") { putBoolean("rollover", v) } }
    fun setWeekStartsOn(v: Int) { weekStartsOn = v; saved("weekStartsOn") { putInt("weekStartsOn", v) } }
    fun setCelebration(v: Boolean) { celebration = v; saved("celebration") { putBoolean("celebration", v) } }
    fun setWeekStrip(v: Boolean) { weekStrip = v; saved("weekStrip") { putBoolean("weekStrip", v) } }
    fun setWeekBand(v: Boolean) { weekBand = v; saved("weekBand") { putBoolean("weekBand", v) } }
    fun setDigestEnabled(v: Boolean) { digestEnabled = v; saved("digestEnabled") { putBoolean("digestEnabled", v) } }
    fun setDigestTime(v: String) { digestTime = v; saved("digestTime") { putString("digestTime", v) } }
    fun setDefaultCountdownAlerts(v: List<Int>) { defaultCountdownAlerts = v; saved("defaultCountdownAlerts") { putString("defaultCountdownAlerts", v.joinToString(",")) } }
    fun setWidgetShowsTodos(v: Boolean) { widgetShowsTodos = v; saved("widgetShowsTodos") { putBoolean("widgetShowsTodos", v) } }
    fun setFullScreenAlerts(v: Boolean) { fullScreenAlerts = v; saved("fullScreenAlerts") { putBoolean("fullScreenAlerts", v) } }
}
