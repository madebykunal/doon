package com.madebykunal.doon.ui.settings

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import com.madebykunal.doon.state.Nav
import com.madebykunal.doon.state.Route
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import com.madebykunal.doon.state.Dialogs
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.state.Snack
import com.madebykunal.doon.state.Ui
import com.madebykunal.doon.ui.components.Choice
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
import com.madebykunal.doon.ui.theme.FONTS
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.HIT_TARGET
import com.madebykunal.doon.ui.theme.headingSample
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

private val CATEGORIES = listOf(
    "general" to "General",
    "appearance" to "Appearance",
    "today" to "Today",
    "lists" to "Lists",
    "soon" to "Soon",
    "habits" to "Habits",
    "permissions" to "Permissions",
    "about" to "About",
)

@Composable
fun SettingsScreen() {
    val c = colors
    val categories = if (BuildConfig.DEBUG) CATEGORIES + ("developer" to "Developer") else CATEGORIES
    Ui.resumes
    val missing = missingPermissions()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings", onBar = { Nav.popScreen() }) {
            val turn = remember { Animatable(180f) }
            LaunchedEffect(Unit) { turn.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 300f)) }
            Box(
                Modifier.size(HIT_TARGET).tap(pressedAlpha = 0.6f, pressedScale = 0.82f, label = "Close settings") { Nav.popScreen() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Ic.ChevronUp, c.inkMuted, 26.dp, modifier = Modifier.graphicsLayer { rotationZ = turn.value })
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = Space.s)) {
            categories.forEachIndexed { i, (id, label) ->
                val shown = remember { Animatable(0f) }
                LaunchedEffect(Unit) { shown.animateTo(1f, tween(260, delayMillis = 30 * i, easing = FastOutSlowInEasing)) }
                Row(
                    Modifier.fillMaxWidth()
                        .graphicsLayer { alpha = shown.value; translationX = (1f - shown.value) * 40.dp.toPx() }
                        .tap(pressedColor = c.hairline, label = label) { Nav.push(Route.SettingsPage(id)) }
                        .heightIn(min = 64.dp).padding(horizontal = GUTTER),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    Txt(label.uppercase(), Type.listName.copy(fontSize = 30.sp, lineHeight = 36.sp), modifier = Modifier.weight(1f), maxLines = 1)
                    if (id == "permissions" && missing > 0) Box(Modifier.size(8.dp).background(c.accentText, CircleShape))
                    Icon(Ic.ChevronRight, c.inkMuted, 22.dp)
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(Space.xxxl))
        }
    }
}

@Composable
fun SettingsPage(id: String) {
    val title = (CATEGORIES + ("developer" to "Developer")).firstOrNull { it.first == id }?.second ?: ""
    var colorPicker by remember { mutableStateOf(false) }
    Ui.resumes
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title, back = true)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                when (id) {
                    "general" -> GeneralPage()
                    "appearance" -> AppearancePage { colorPicker = true }
                    "today" -> {
                        SwitchRow("Block tab bar with counts", Prefs.tabStyle == "block", "A filled block marks your tab, with how much is waiting in each") {
                            Prefs.setTabStyle(if (it) "block" else "classic")
                        }
                        SwitchRow("Week strip at the top", Prefs.weekStripTop, "Shows the days just under the date", Prefs::setWeekStripTop)
                        SwitchRow("Roll over unfinished to-dos", Prefs.rollover, "Moves them to today when a new day starts", Prefs::setRollover)
                        SwitchRow("Celebrate a cleared day", Prefs.celebration, null, Prefs::setCelebration)
                    }
                    "lists" -> {
                        SwitchRow("Count on collapsed folders", Prefs.listFolderCounts, "How many lists a closed folder holds", Prefs::setListFolderCounts)
                        SwitchRow("Count on lists", Prefs.listCounts, "How many open to-dos each list has", Prefs::setListCounts)
                        SwitchRow("Big counts", Prefs.listBigCounts, "Counts in the heading font, larger", Prefs::setListBigCounts)
                    }
                    "soon" -> SwitchRow("Show past countdowns", Prefs.soonShowPast, "Keeps finished ones under Past", Prefs::setSoonShowPast)
                    "habits" -> SwitchRow("Show the last 7 days", Prefs.habitWeekDots, "A row of dots on each habit", Prefs::setHabitWeekDots)
                    "permissions" -> PermissionsPage()
                    "about" -> {
                        SettingRow("Version", right = { Txt(BuildConfig.VERSION_NAME, Type.secondary, colors.inkMuted) })
                        SettingRow("Your data", "Everything stays on this phone. Doon has no account, no tracking and no internet access.")
                        SettingRow("Onboarding", "See the welcome page again", right = { Icon(Ic.ChevronRight, colors.inkMuted, 20.dp) }) { Prefs.setOnboarded(false) }
                    }
                    "developer" -> SettingRow("Seed performance data", "2 000 to-dos, 30 lists, 50 countdowns") {
                        Seed.performance(Ui.today)
                        Snack.show("Seeded")
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
private fun GeneralPage() {
    val c = colors
    SettingRow("Week starts on", right = { Txt(Dates.format(Dates.addDays("2026-11-01", Prefs.weekStart), "EEEE"), Type.secondary, c.accentText) }) {
        Prefs.setWeekStartsOn((Prefs.weekStart + 1) % 7)
    }
    SwitchRow("Completed to-dos go to the bottom", Prefs.sinkCompleted, "Keeps what’s left at the top", Prefs::setSinkCompleted)
    SwitchRow("Full-screen alerts", Prefs.fullScreenAlerts, "Reminders wake the screen and show over other apps") { on ->
        Prefs.setFullScreenAlerts(on)
        Reminders.resyncAll()
        if (on && !Reminders.canFullScreen()) Reminders.openFullScreenSettings()
        else if (on && !Reminders.canOverlay()) Reminders.requestOverlay()
    }
    SubLabel("Sounds")
    SoundRow("To-do reminders", "Doon chime", Prefs.taskSound, Prefs::setTaskSound)
    SoundRow("Countdowns", "Doon bell", Prefs.countdownSound, Prefs::setCountdownSound)
    if (missingPermissions() > 0) {
        SettingRow("Some permissions are off", "Reminders may be late or quiet", right = { Txt("Review", Type.secondaryStrong, c.accentText) }) {
            Nav.push(Route.SettingsPage("permissions"))
        }
    }
}

@Composable
private fun SoundRow(label: String, doonName: String, value: String, set: (String) -> Unit) {
    val c = colors
    val name = remember(value) { Reminders.soundName(value, doonName) }
    val apply = { v: String ->
        set(v)
        Reminders.soundsChanged()
    }
    SettingRow(label, right = { Txt(name, Type.secondary, c.accentText, Modifier.widthIn(max = 160.dp), maxLines = 1) }) {
        Dialogs.show(
            "$label sound",
            actions = listOf(
                DialogAction("Choose from phone", "primary") { Reminders.pickSound(value) { apply(it) } },
                DialogAction(doonName) { apply("") },
                DialogAction("Silent") { apply(Reminders.SILENT) },
                DialogAction("Cancel", "cancel"),
            ),
        )
    }
}

@Composable
private fun AppearancePage(onCustom: () -> Unit) {
    val c = colors
    SubLabel("Accent")
    val ids = Accents.presets.keys.toList() + "custom"
    Column(Modifier.padding(horizontal = Space.s, vertical = Space.s)) {
        ids.chunked(6).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { id ->
                    Box(
                        Modifier.weight(1f).height(56.dp).tap(label = Accents.presets[id]?.first ?: "Custom accent color") {
                            if (id == "custom") onCustom() else Prefs.setAccent(id)
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        val selected = (Accents.renamed[Prefs.accent] ?: Prefs.accent) == id
                        val (fill, on) = c.accentFor(if (id == "custom") Prefs.customColor else Accents.presets.getValue(id).second)
                        val ring = if (selected) Modifier.border(2.dp, c.ink, CircleShape).padding(4.dp) else Modifier
                        Box(Modifier.size(40.dp).then(ring), contentAlignment = Alignment.Center) {
                            if (id == "custom" && Prefs.accent != "custom") {
                                Box(Modifier.fillMaxSize().border(1.5.dp, c.inkMuted, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Ic.Plus, c.inkMuted, 16.dp)
                                }
                            } else {
                                Box(Modifier.fillMaxSize().background(fill, CircleShape), contentAlignment = Alignment.Center) {
                                    if (selected) Icon(Ic.Check, on, 16.dp, stroke = 3f)
                                }
                            }
                        }
                    }
                }
                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    SubLabel("Theme")
    Column(Modifier.fillMaxWidth().padding(horizontal = GUTTER, vertical = Space.s), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        THEMES.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pair.forEach { (mode, label) -> Choice(label, Prefs.themeMode == mode, Modifier.weight(1f)) { Prefs.setThemeMode(mode) } }
            }
        }
    }
    SubLabel("Font")
    Row(Modifier.fillMaxWidth().padding(horizontal = GUTTER, vertical = Space.s), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FONTS.forEach { (id, label) ->
            Choice(label, (FONTS.firstOrNull { it.first == Prefs.font }?.first ?: "condensed") == id, Modifier.weight(1f), style = headingSample(id).copy(fontSize = 18.sp, lineHeight = 22.sp)) { Prefs.setFont(id) }
        }
    }
    Txt("Changes headings, numbers and the tab bar.", Type.caption, c.inkMuted, Modifier.padding(horizontal = GUTTER).padding(top = Space.xs, bottom = Space.s))
    SubLabel("Add button")
    Row(Modifier.fillMaxWidth().padding(horizontal = GUTTER, vertical = Space.s), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("circle" to "Circle", "square" to "Square", "block" to "Block").forEach { (id, label) ->
            Choice(label, Prefs.plusShape == id, Modifier.weight(1f)) { Prefs.setPlusShape(id) }
        }
    }
    SubLabel("Week strip")
    Row(Modifier.fillMaxWidth().padding(horizontal = GUTTER, vertical = Space.s), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("circle" to "Round days", "square" to "Square days").forEach { (id, label) ->
            Choice(label, Prefs.stripShape == id, Modifier.weight(1f)) { Prefs.setStripShape(id) }
        }
    }
    SwitchRow("Hide the week strip", Prefs.weekStripHidden, "Tap the week mark next to the date to show it") {
        Prefs.setWeekStripHidden(it)
        Ui.stripOpen = false
    }
}

private fun missingPermissions(): Int {
    var n = 0
    if (!Reminders.hasPermission()) n++
    if (!Reminders.canExact()) n++
    if (Prefs.fullScreenAlerts && !Reminders.canFullScreen()) n++
    if (Prefs.fullScreenAlerts && !Reminders.canOverlay()) n++
    return n
}

@Composable
private fun PermissionsPage() {
    val c = colors
    Txt(
        "Doon asks for each of these only when a feature needs it. You can change them any time in Android settings.",
        Type.secondary, c.inkMuted, Modifier.padding(horizontal = GUTTER).padding(top = Space.xs, bottom = Space.l),
    )
    PermissionRow("Notifications", "Shows your to-do reminders, countdown alerts and the morning summary.", Reminders.hasPermission()) {
        Reminders.openNotificationSettings()
    }
    PermissionRow("Alarms and reminders", "Lets a reminder ring on the exact minute instead of a few minutes late.", Reminders.canExact()) {
        Reminders.openExactAlarmSettings()
    }
    PermissionRow(
        "Full-screen notifications",
        "Lets an alert wake a locked phone, like an alarm clock. Only used when Full-screen alerts is on.",
        Reminders.canFullScreen(),
        needed = Prefs.fullScreenAlerts,
    ) { Reminders.openFullScreenSettings() }
    PermissionRow(
        "Display over other apps",
        "Lets an alert appear on top of the app you’re using. Only used when Full-screen alerts is on.",
        Reminders.canOverlay(),
        needed = Prefs.fullScreenAlerts,
    ) { Reminders.requestOverlay() }
    PermissionRow("Run at startup", "Puts your reminders back after the phone restarts. Android grants this at install.", true)
    SubLabel("Never asked for")
    PermissionRow("Internet", "Doon can’t go online. Your to-dos, lists and habits never leave this phone.", false, never = true)
}

@Composable
private fun PermissionRow(title: String, reason: String, granted: Boolean, needed: Boolean = true, never: Boolean = false, onOpen: (() -> Unit)? = null) {
    val c = colors
    val canAct = onOpen != null && !granted
    Row(
        Modifier.fillMaxWidth().then(if (canAct) Modifier.tap(pressedColor = c.hairline, label = "Allow $title") { onOpen?.invoke() } else Modifier)
            .padding(horizontal = GUTTER, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Txt(title, Type.bodyStrong)
            Txt(reason, Type.caption, c.inkMuted)
        }
        val word = Type.nav.copy(fontSize = 16.sp, lineHeight = 18.sp, letterSpacing = 0.6.sp)
        when {
            never -> Txt("NONE", word, c.inkMuted, unscaled = true)
            granted -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Ic.Check, c.accentText, 14.dp, stroke = 3f)
                Txt("ON", word, c.accentText, unscaled = true)
            }
            !needed -> Txt("OFF", word, c.inkMuted, unscaled = true)
            else -> Box(Modifier.background(c.accent).padding(horizontal = Space.m, vertical = 6.dp)) {
                Txt("ALLOW", word, c.onAccent, unscaled = true)
            }
        }
    }
}

@Composable
private fun SubLabel(text: String) {
    Txt(text.uppercase(), Type.label, colors.inkMuted, Modifier.padding(horizontal = GUTTER).padding(top = Space.xl, bottom = Space.xs))
}

@Composable
private fun SettingRow(label: String, detail: String? = null, right: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null) {
    val c = colors
    var m = Modifier.fillMaxWidth()
    if (onClick != null) m = m.tap(pressedColor = c.hairline, onClick = onClick)
    Row(
        m.heightIn(min = 60.dp).padding(horizontal = GUTTER, vertical = 10.dp),
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
    val view = androidx.compose.ui.platform.LocalView.current
    SettingRow(label, detail, right = { Toggle(value) }) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        onChange(!value)
    }
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
            Box(Modifier.size(32.dp).clip(RectangleShape).background(preview))
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
                Modifier.offset { IntOffset((tx - half).roundToInt(), (ty - half).roundToInt()) }.size(thumb).shadow(3.dp, RectangleShape)
                    .background(hex(ColorMath.hsvToHex(h, s, 1.0)), RectangleShape).border(3.dp, Color.White, RectangleShape),
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
                Modifier.fillMaxWidth().height(14.dp).clip(RectangleShape).background(
                    Brush.horizontalGradient(listOf(hex(ColorMath.hsvToHex(h, s, MIN_BRIGHTNESS)), hex(ColorMath.hsvToHex(h, s, 1.0)))),
                ),
            )
            val x = ((v - MIN_BRIGHTNESS) / (1 - MIN_BRIGHTNESS)) * wheelPx
            val half = with(density) { (thumb / 2).toPx() }
            Box(
                Modifier.offset { IntOffset((x - half).roundToInt(), 0) }.size(thumb).shadow(3.dp, RectangleShape)
                    .background(hex(hexValue), RectangleShape).border(3.dp, Color.White, RectangleShape),
            )
        }
        Row(Modifier.padding(top = Space.l), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            DialogButton(DialogAction("Cancel", "cancel"), Modifier.weight(1f), onCancel)
            DialogButton(DialogAction("Use color", "primary"), Modifier.weight(1f)) { onPick(hexValue) }
        }
    }
}
