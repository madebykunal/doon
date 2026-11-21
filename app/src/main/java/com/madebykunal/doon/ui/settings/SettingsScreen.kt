package com.madebykunal.doon.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.madebykunal.doon.BuildConfig
import com.madebykunal.doon.data.Seed
import com.madebykunal.doon.lib.ColorMath
import com.madebykunal.doon.lib.CountdownMath
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.notify.Reminders
import com.madebykunal.doon.state.DialogAction
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.state.Snack
import com.madebykunal.doon.state.Ui
import com.madebykunal.doon.ui.components.DialogButton
import com.madebykunal.doon.ui.components.DialogShell
import com.madebykunal.doon.ui.components.Ic
import com.madebykunal.doon.ui.components.Icon
import com.madebykunal.doon.ui.components.ScreenHeader
import com.madebykunal.doon.ui.components.TimePicker
import com.madebykunal.doon.ui.components.Toggle
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.hairlineTop
import com.madebykunal.doon.ui.components.tap
import com.madebykunal.doon.ui.theme.Accents
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.RADIUS_CARD
import com.madebykunal.doon.ui.theme.ROW_MIN_HEIGHT
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import com.madebykunal.doon.ui.theme.hex
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val THEMES = listOf("system" to "System", "light" to "Light", "dark" to "Dark", "black" to "Pure black")

@Composable
fun SettingsScreen() {
    val c = colors
    var digestPicker by remember { mutableStateOf(false) }
    var colorPicker by remember { mutableStateOf(false) }
    Ui.resumes
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Settings", back = true)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.l), verticalArrangement = Arrangement.spacedBy(Space.xl)) {
                Section("Accent") {
                    Column(Modifier.padding(Space.s)) {
                        val ids = Accents.presets.keys.toList() + "custom"
                        ids.chunked(6).forEach { row ->
                            Row(Modifier.fillMaxWidth()) {
                                row.forEach { id ->
                                    Box(
                                        Modifier.weight(1f).height(52.dp).tap(label = Accents.presets[id]?.first ?: "Custom accent color") {
                                            if (id == "custom") colorPicker = true else Prefs.setAccent(id)
                                        },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        val selected = Prefs.accent == id
                                        if (id == "custom") {
                                            val (fill, on) = c.accentFor(Prefs.customColor)
                                            val m = if (selected) Modifier.background(fill, CircleShape) else Modifier.border(1.5.dp, c.inkMuted, CircleShape)
                                            Box(Modifier.size(36.dp).clip(CircleShape).then(m), contentAlignment = Alignment.Center) {
                                                Icon(Ic.Pencil, if (selected) on else c.ink, 16.dp)
                                            }
                                        } else {
                                            val (fill, on) = c.accentFor(Accents.presets.getValue(id).second)
                                            Box(Modifier.size(36.dp).clip(CircleShape).background(fill), contentAlignment = Alignment.Center) {
                                                if (selected) Icon(Ic.Check, on, 18.dp, stroke = 3f)
                                            }
                                        }
                                    }
                                }
                                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                Section("Theme") {
                    Row(Modifier.padding(Space.xs), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        THEMES.forEach { (mode, label) ->
                            val selected = Prefs.themeMode == mode
                            Box(
                                Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(RADIUS_CARD - Space.xs))
                                    .background(if (selected) c.accentTint else Color.Transparent).tap(label = label) { Prefs.setThemeMode(mode) },
                                contentAlignment = Alignment.Center,
                            ) { Txt(label, Type.captionStrong, if (selected) c.accentText else c.inkMuted, maxLines = 1) }
                        }
                    }
                }
                Section("Preferences") {
                    SwitchRow("Hide completed", Prefs.hideCompleted, null, Prefs::setHideCompleted)
                    SwitchRow("Completed sink to the bottom", Prefs.sinkCompleted, null, Prefs::setSinkCompleted)
                    SwitchRow("Roll over unfinished to-dos", Prefs.rollover, "Moves them to today when a new day starts", Prefs::setRollover)
                    SwitchRow("Show the week strip", Prefs.weekStrip, "The row of days under the date", Prefs::setWeekStrip)
                    if (Prefs.weekStrip) SwitchRow("Tint the week strip", Prefs.weekBand, "A soft band with fine lines behind it", Prefs::setWeekBand)
                    SwitchRow("Celebrate a cleared day", Prefs.celebration, null, Prefs::setCelebration)
                    SwitchRow("Show to-dos on the widget", Prefs.widgetShowsTodos, "When off, the widget shows only how many", Prefs::setWidgetShowsTodos)
                    SettingRow("Week starts on", right = { Txt(Dates.format(Dates.addDays("2026-11-01", Prefs.weekStart), "EEEE"), color = c.inkMuted) }) {
                        Prefs.setWeekStartsOn((Prefs.weekStart + 1) % 7)
                    }
                }
                RemindersSection()
                Section("Morning digest") {
                    SwitchRow("Send a morning digest", Prefs.digestEnabled, "Today’s to-dos and your next countdown") {
                        Prefs.setDigestEnabled(it)
                        Reminders.syncDigest(ask = it)
                    }
                    if (Prefs.digestEnabled) {
                        SettingRow("Time", right = { Txt(Prefs.digestTime, color = c.inkMuted) }) { digestPicker = !digestPicker }
                        if (digestPicker) {
                            Box(Modifier.padding(vertical = Space.s)) {
                                TimePicker(Prefs.digestTime) {
                                    Prefs.setDigestTime(it)
                                    Reminders.syncDigest()
                                }
                            }
                        }
                    }
                }
                Section("New countdowns alert me") {
                    CountdownMath.alerts.forEach { (offset, label) ->
                        val on = offset in Prefs.defaultCountdownAlerts
                        SwitchRow(label, on, null) {
                            Prefs.setDefaultCountdownAlerts(if (on) Prefs.defaultCountdownAlerts - offset else Prefs.defaultCountdownAlerts + offset)
                        }
                    }
                }
                Section("About") {
                    SettingRow("Version", right = { Txt(BuildConfig.VERSION_NAME, color = c.inkMuted) })
                }
                if (BuildConfig.DEBUG) {
                    Section("Developer") {
                        SettingRow("Seed performance data", "2 000 to-dos, 30 lists, 50 countdowns") {
                            Seed.performance(Ui.today)
                            Snack.show("Seeded")
                        }
                    }
                }
                Spacer(Modifier.navigationBarsPadding().height(Space.xxxl))
            }
        }
        if (colorPicker) {
            ColorPicker(Prefs.customColor, onCancel = { colorPicker = false }) {
                Prefs.setCustomColor(it)
                Prefs.setAccent("custom")
                colorPicker = false
            }
        }
    }
}

@Composable
private fun RemindersSection() {
    val c = colors
    Ui.resumes
    val exact = Reminders.canExact()
    val fullScreen = Reminders.canFullScreen()
    val overlay = Reminders.canOverlay()
    val action = @Composable { label: String -> Txt(label, color = c.accentText) }
    Section("Reminders") {
        if (!exact) SettingRow("Ring on the minute", "Allow alarms so Android doesn’t hold reminders back", right = { action("Allow") }) { Reminders.openExactAlarmSettings() }
        SwitchRow("Full-screen alerts", Prefs.fullScreenAlerts, "Reminders wake the screen and show over other apps") { on ->
            Prefs.setFullScreenAlerts(on)
            Reminders.resyncAll()
            if (on && !fullScreen) Reminders.openFullScreenSettings()
            else if (on && !overlay) Reminders.openOverlaySettings()
        }
        if (Prefs.fullScreenAlerts && !fullScreen) SettingRow("Allow full-screen notifications", "Needed to wake a locked phone", right = { action("Open") }) { Reminders.openFullScreenSettings() }
        if (Prefs.fullScreenAlerts && !overlay) SettingRow("Allow display over other apps", "Needed to show over the app you’re using", right = { action("Open") }) { Reminders.openOverlaySettings() }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = colors
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Txt(title, Type.captionStrong, c.inkMuted, Modifier.padding(horizontal = GUTTER - Space.l))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(RADIUS_CARD)).background(c.surface).border(1.dp, c.hairline, RoundedCornerShape(RADIUS_CARD)),
            content = content,
        )
    }
}

@Composable
private fun SettingRow(label: String, detail: String? = null, right: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null) {
    val c = colors
    var m = Modifier.fillMaxWidth()
    if (onClick != null) m = m.tap(pressedColor = c.hairline, onClick = onClick)
    Row(
        m.heightIn(min = ROW_MIN_HEIGHT).hairlineTop(c.hairline).padding(horizontal = Space.l, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(label)
            if (detail != null) Txt(detail, Type.caption, c.inkMuted)
        }
        right?.invoke()
    }
}

@Composable
private fun SwitchRow(label: String, value: Boolean, detail: String?, onChange: (Boolean) -> Unit) {
    SettingRow(label, detail, right = { Toggle(value) }) { onChange(!value) }
}

private const val MIN_BRIGHTNESS = 0.2

@Composable
private fun ColorPicker(initial: String, onCancel: () -> Unit, onPick: (String) -> Unit) {
    val c = colors
    val start = remember { ColorMath.hexToHsv(initial) }
    var h by remember { mutableStateOf(start[0]) }
    var s by remember { mutableStateOf(start[1]) }
    var v by remember { mutableStateOf(start[2]) }
    val hexValue = ColorMath.hsvToHex(h, s, v)
    val preview = c.accentFor(hexValue).first
    val wheel = 232.dp
    val thumb = 26.dp
    val density = LocalDensity.current
    val wheelPx = with(density) { wheel.toPx() }
    val center = wheelPx / 2
    val hues = remember { (0..12).map { hex(ColorMath.hsvToHex((it * 30 % 360).toDouble(), 1.0, 1.0)) } }

    DialogShell(onCancel) {
        Row(Modifier.fillMaxWidth().padding(bottom = Space.l), verticalAlignment = Alignment.CenterVertically) {
            Txt("Custom accent", Type.h2, modifier = Modifier.weight(1f))
            Box(Modifier.size(32.dp).clip(CircleShape).background(preview))
        }
        Box(
            Modifier.align(Alignment.CenterHorizontally).size(wheel).pointerInput(Unit) {
                awaitEachGesture {
                    fun move(p: Offset) {
                        val dx = p.x - center
                        val dy = p.y - center
                        val distance = min(center, sqrt(dx * dx + dy * dy))
                        h = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 360) % 360)
                        s = (distance / center).toDouble()
                    }
                    val down = awaitFirstDown()
                    move(down.position)
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { if (it.positionChange() != Offset.Zero) it.consume() }
                        event.changes.firstOrNull()?.let { move(it.position) }
                    } while (event.changes.any { it.pressed })
                }
            },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Brush.sweepGradient(hues), center)
                drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f))), center)
            }
            val rad = Math.toRadians(h)
            val tx = center + cos(rad) * s * center
            val ty = center + sin(rad) * s * center
            val half = with(density) { (thumb / 2).toPx() }
            Box(
                Modifier.offset { IntOffset((tx - half).roundToInt(), (ty - half).roundToInt()) }.size(thumb).shadow(3.dp, CircleShape)
                    .background(hex(ColorMath.hsvToHex(h, s, 1.0)), CircleShape).border(3.dp, Color.White, CircleShape),
            )
        }
        Box(
            Modifier.align(Alignment.CenterHorizontally).padding(top = Space.l).width(wheel).height(thumb + Space.s).pointerInput(Unit) {
                awaitEachGesture {
                    fun move(x: Float) {
                        val clamped = x.coerceIn(0f, wheelPx)
                        v = MIN_BRIGHTNESS + (clamped / wheelPx) * (1 - MIN_BRIGHTNESS)
                    }
                    val down = awaitFirstDown()
                    move(down.position.x)
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        event.changes.firstOrNull()?.let { move(it.position.x) }
                    } while (event.changes.any { it.pressed })
                }
            },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(
                    Brush.horizontalGradient(listOf(hex(ColorMath.hsvToHex(h, s, MIN_BRIGHTNESS)), hex(ColorMath.hsvToHex(h, s, 1.0)))),
                ),
            )
            val x = ((v - MIN_BRIGHTNESS) / (1 - MIN_BRIGHTNESS)) * wheelPx
            val half = with(density) { (thumb / 2).toPx() }
            Box(
                Modifier.offset { IntOffset((x - half).roundToInt(), 0) }.size(thumb).shadow(3.dp, CircleShape)
                    .background(hex(hexValue), CircleShape).border(3.dp, Color.White, CircleShape),
            )
        }
        Row(Modifier.padding(top = Space.l), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            DialogButton(DialogAction("Cancel", "cancel"), Modifier.weight(1f), onCancel)
            DialogButton(DialogAction("Use color", "primary"), Modifier.weight(1f)) { onPick(hexValue) }
        }
    }
}
