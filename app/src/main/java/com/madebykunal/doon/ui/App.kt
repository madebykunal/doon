package com.madebykunal.doon.ui

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.lerp
import com.madebykunal.doon.data.HabitRepo
import com.madebykunal.doon.data.Live
import com.madebykunal.doon.data.T
import com.madebykunal.doon.ui.components.hairlineTop
import com.madebykunal.doon.ui.onboarding.Intro
import com.madebykunal.doon.ui.onboarding.Onboarding
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import com.madebykunal.doon.state.DialogAction
import com.madebykunal.doon.state.Dialogs
import com.madebykunal.doon.ui.today.WeekStripBar
import com.madebykunal.doon.ui.todos.QuickAdd
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.madebykunal.doon.data.CountdownRepo
import com.madebykunal.doon.data.Home
import com.madebykunal.doon.data.ListRepo
import com.madebykunal.doon.data.TodoRepo
import com.madebykunal.doon.data.Where
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.Markdown
import com.madebykunal.doon.state.Editor
import com.madebykunal.doon.state.Nav
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.state.Route
import com.madebykunal.doon.state.SheetRoute
import com.madebykunal.doon.state.Ui
import com.madebykunal.doon.ui.components.DialogHost
import com.madebykunal.doon.ui.components.Field
import com.madebykunal.doon.ui.components.OptionRow
import com.madebykunal.doon.ui.components.ScreenHeader
import com.madebykunal.doon.ui.components.SectionLabel
import com.madebykunal.doon.ui.components.SheetHost
import com.madebykunal.doon.ui.components.SnackbarHost
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.tap
import com.madebykunal.doon.ui.countdowns.CountdownDetailScreen
import com.madebykunal.doon.ui.countdowns.CountdownsScreen
import com.madebykunal.doon.ui.habits.HabitDetailScreen
import com.madebykunal.doon.ui.habits.HabitsScreen
import com.madebykunal.doon.ui.lists.ListDetailScreen
import com.madebykunal.doon.ui.lists.ListsScreen
import com.madebykunal.doon.ui.settings.SettingsPage
import com.madebykunal.doon.ui.settings.SettingsScreen
import com.madebykunal.doon.ui.sheets.SheetContent
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.TOOLBAR_HEIGHT
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import com.madebykunal.doon.ui.today.TodayScreen
import com.madebykunal.doon.ui.todos.DragGhost
import kotlinx.coroutines.delay

fun pressPlus() {
    when (val route = Nav.stacks[Nav.tab].lastOrNull()) {
        is Route.ListDetail -> Editor.openComposer(Home.InList(route.id), Where.Bottom)
        is Route.CountdownDetail -> Nav.openSheet(SheetRoute.CountdownEditor())
        is Route.HabitDetail -> Nav.openSheet(SheetRoute.HabitEditor())
        else -> when (Nav.tab) {
            0 -> Editor.openComposer(Home.Day(Ui.selectedDate), Where.Bottom)
            1 -> Dialogs.show(
                "Create",
                actions = listOf(
                    DialogAction("New list", "primary") { Nav.openSheet(SheetRoute.ListEditor(mode = "list")) },
                    DialogAction("New folder") { Nav.openSheet(SheetRoute.ListEditor(mode = "group")) },
                    DialogAction("Cancel", "cancel"),
                ),
            )
            2 -> Nav.openSheet(SheetRoute.CountdownEditor())
            else -> Nav.openSheet(SheetRoute.HabitEditor())
        }
    }
}

private val TAB_LABELS = listOf("Today", "Lists", "Soon", "Habits")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun App() {
    val c = colors
    val imeVisible = WindowInsets.isImeVisible
    var wasIme by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (wasIme && !imeVisible && Nav.sheets.isEmpty()) Editor.commitAll()
        wasIme = imeVisible
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Dates.msUntilNextMidnight() + 50)
            com.madebykunal.doon.DoonApp.dayChanged()
        }
    }
    BackHandler(Nav.canGoBack()) { Nav.back() }

    val overlay = Nav.overlay.lastOrNull()
    val view = LocalView.current
    val deepIsDark = c.onAccentDeep == Color.White
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = if (overlay == null && Prefs.tabStyle != "block") !deepIsDark else !c.dark
    }
    Box(Modifier.fillMaxSize().background(c.bg)) {
        AnimatedContent(
            targetState = Nav.overlay.size to overlay,
            transitionSpec = {
                val from = initialState.first
                val to = targetState.first
                val ease = androidx.compose.animation.core.FastOutSlowInEasing
                when {
                    to > from && from == 0 -> slideInVertically(tween(320, easing = ease)) { -it } togetherWith fadeOut(tween(260))
                    to < from && to == 0 -> fadeIn(tween(220)) togetherWith slideOutVertically(tween(280, easing = ease)) { -it }
                    to > from -> slideInHorizontally(tween(220)) { it } togetherWith fadeOut(tween(220))
                    else -> fadeIn(tween(160)) togetherWith slideOutHorizontally(tween(200)) { it }
                }
            },
            label = "overlay",
        ) { (_, route) ->
            when (route) {
                Route.Settings -> SettingsScreen()
                is Route.SettingsPage -> SettingsPage(route.id)
                else -> Shell(imeVisible)
            }
        }
        DragGhost()
        QuickAdd()
        SheetHost { SheetContent(it) }
        DialogHost()
        if (!Prefs.onboarded) Onboarding()
        Intro()
    }
}

@Composable
private fun Shell(imeVisible: Boolean) {
    val c = colors
    val onToday = Nav.tab == 0 && Nav.stacks[0].isEmpty()
    Column(Modifier.fillMaxSize().imePadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Tabs()
            if (!imeVisible && Editor.composer == null) {
                FloatingAdd()
            }
            SnackbarHost(if (imeVisible) TOOLBAR_HEIGHT + 8.dp else 92.dp)
        }
        if (!imeVisible) {
            androidx.compose.animation.AnimatedVisibility(
                onToday && !Prefs.weekStripTop,
                enter = androidx.compose.animation.expandVertically(tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + fadeIn(tween(200)),
                exit = androidx.compose.animation.shrinkVertically(tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + fadeOut(tween(140)),
            ) { WeekStripBar() }
            BottomNav()
        }
    }
}

private val fabOffset = Animatable(Offset.Zero, Offset.VectorConverter)

@Composable
private fun FloatingAdd() {
    val c = colors
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val spin = remember { Animatable(-135f) }
    val scale = remember { Animatable(0.4f) }
    var dragging by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(if (dragging) 1.12f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "lift")
    LaunchedEffect(Nav.tab) {
        launch { scale.snapTo(0.6f); scale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 420f)) }
        spin.snapTo(-90f)
        spin.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 260f))
    }
    BoxWithConstraints(Modifier.fillMaxSize().padding(Space.l), contentAlignment = Alignment.BottomEnd) {
        val size = 60.dp
        val maxX = with(density) { (maxWidth - size).toPx() }
        val maxY = with(density) { (maxHeight - size).toPx() }
        val snap = with(density) { 96.dp.toPx() }
        val home = fabOffset.value.getDistance() < snap
        Box(
            Modifier.offset { IntOffset(fabOffset.value.x.roundToInt(), fabOffset.value.y.roundToInt()) }
                .size(size)
                .graphicsLayer { val s = scale.value * lift; scaleX = s; scaleY = s }
                .then(
                    when (Prefs.plusShape) {
                        "square" -> Modifier.shadow(if (home && !dragging) 6.dp else 12.dp, RectangleShape).background(c.accent)
                        "block" -> Modifier.drawBehind {
                            val d = (if (dragging) 9.dp else 6.dp).toPx()
                            drawRect(c.accentDeep, Offset(d, d), this.size)
                        }.background(c.accent)
                        else -> Modifier.shadow(if (home && !dragging) 6.dp else 12.dp, CircleShape).background(c.accent, CircleShape)
                    },
                )
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragCancel = { dragging = false },
                        onDragEnd = {
                            dragging = false
                            scope.launch {
                                if (fabOffset.value.getDistance() < snap) fabOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.7f, stiffness = 400f))
                            }
                        },
                    ) { change, amount ->
                        change.consume()
                        val next = fabOffset.value + amount
                        scope.launch { fabOffset.snapTo(Offset(next.x.coerceIn(-maxX, 0f), next.y.coerceIn(-maxY, 0f))) }
                        scope.launch { spin.snapTo(spin.value + amount.x * 0.6f) }
                    }
                }
                .tap(haptic = true, label = "Add") {
                    scope.launch {
                        launch { scale.animateTo(0.82f, tween(70)); scale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f)) }
                        spin.animateTo(spin.targetValue.roundTo90() + 90f, spring(dampingRatio = 0.45f, stiffness = 320f))
                    }
                    pressPlus()
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(24.dp).graphicsLayer { rotationZ = spin.value }) {
                val w = 2.75.dp.toPx()
                val mid = this.size.width / 2
                drawLine(c.onAccent, Offset(0f, mid), Offset(this.size.width, mid), w, StrokeCap.Butt)
                drawLine(c.onAccent, Offset(mid, 0f), Offset(mid, this.size.height), w, StrokeCap.Butt)
            }
        }
    }
}

private fun Float.roundTo90(): Float = Math.round(this / 90f) * 90f

@Composable
private fun BottomNav() {
    if (Prefs.tabStyle == "block") BlockNav() else ClassicNav()
}

private fun selectTab(index: Int) {
    if (Nav.tab == index) Nav.stacks[index].clear()
    Nav.selectTab(index)
}

@Composable
private fun ClassicNav() {
    val c = colors
    Row(Modifier.fillMaxWidth().background(c.accentDeep).navigationBarsPadding().height(80.dp)) {
        TAB_LABELS.forEachIndexed { index, label ->
            val selected = Nav.tab == index
            Column(
                Modifier.weight(1f).fillMaxHeight().tap(pressedScale = 0.88f, haptic = true, label = label) { selectTab(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val dot by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "dot")
                Txt(label.uppercase(), Type.nav, c.onAccentDeep.copy(alpha = 0.7f + 0.3f * dot.coerceIn(0f, 1f)), maxLines = 1, unscaled = true)
                Box(Modifier.padding(top = 4.dp).size(5.dp).graphicsLayer { scaleX = dot; scaleY = dot; alpha = dot }.background(c.onAccentDeep, CircleShape))
            }
        }
    }
}

@Composable
private fun tabCounts(): List<Int> {
    val today = Ui.today
    val todos = Live.version(T.TODOS)
    val countdowns = Live.version(T.COUNTDOWNS)
    val habits = Live.version(T.HABITS)
    val logs = Live.version(T.HABIT_LOGS)
    val onToday = remember(todos, today) { TodoRepo.countOpenOn(today) }
    val inLists = remember(todos) { ListRepo.counts().values.sum() }
    val soon = remember(countdowns, today) { CountdownRepo.countUpcoming(today) }
    val due = remember(habits, logs, today) { HabitRepo.countDue(today) }
    return listOf(onToday, inLists, soon, due)
}

@Composable
private fun BlockNav() {
    val c = colors
    val counts = tabCounts()
    BoxWithConstraints(Modifier.fillMaxWidth().background(c.bg).hairlineTop(c.hairline).navigationBarsPadding().height(76.dp)) {
        val cell = maxWidth / TAB_LABELS.size
        val x by animateDpAsState(cell * Nav.tab, spring(dampingRatio = 0.72f, stiffness = 520f), label = "tabBlock")
        Box(Modifier.offset { IntOffset((x + 6.dp).roundToPx(), 0) }.padding(vertical = 14.dp).width(cell - 12.dp).fillMaxHeight().background(c.accent))
        Row(Modifier.fillMaxSize()) {
            TAB_LABELS.forEachIndexed { index, label ->
                val on by animateFloatAsState(if (Nav.tab == index) 1f else 0f, tween(200), label = "tabInk")
                val ink = lerp(c.inkMuted, c.onAccent, on)
                Row(
                    Modifier.weight(1f).fillMaxHeight().tap(pressedScale = 0.9f, haptic = true, label = label) { selectTab(index) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt(label.uppercase(), Type.nav, ink, maxLines = 1, unscaled = true)
                    val n = counts[index]
                    if (n > 0) Txt(if (n > 99) "99+" else n.toString(), Type.micro, ink, Modifier.padding(start = 2.dp, bottom = 12.dp), tabularNums = true, unscaled = true)
                }
            }
        }
    }
}

@Composable
private fun Tabs() {
    val holder = rememberSaveableStateHolder()
    val tab = Nav.tab
    val route = Nav.stacks[tab].lastOrNull()
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = Triple(tab, route, Nav.stacks[tab].size),
            transitionSpec = {
                val (fromTab, _, fromDepth) = initialState
                val (toTab, _, toDepth) = targetState
                when {
                    fromTab != toTab -> {
                        val dir = if (toTab > fromTab) 1 else -1
                        (slideInHorizontally(tween(220)) { it * dir / 8 } + fadeIn(tween(220))) togetherWith (slideOutHorizontally(tween(180)) { -it * dir / 8 } + fadeOut(tween(140)))
                    }
                    fromDepth == toDepth -> fadeIn(tween(160)) togetherWith fadeOut(tween(120))
                    toDepth > fromDepth -> (slideInHorizontally(tween(240)) { it / 3 } + fadeIn(tween(200))) togetherWith (slideOutHorizontally(tween(240)) { -it / 6 } + fadeOut(tween(180)))
                    else -> (slideInHorizontally(tween(240)) { -it / 6 } + fadeIn(tween(200))) togetherWith (slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(180)))
                } using null
            },
            label = "tabs",
        ) { (t, r, _) ->
            holder.SaveableStateProvider("$t:${r ?: "root"}") {
                when (r) {
                    is Route.ListDetail -> ListDetailScreen(r.id)
                    is Route.CountdownDetail -> CountdownDetailScreen(r.id)
                    is Route.HabitDetail -> HabitDetailScreen(r.id)
                    else -> when (t) {
                        0 -> TodayScreen()
                        1 -> ListsScreen()
                        2 -> CountdownsScreen()
                        else -> HabitsScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun MigrationFailedScreen(error: Throwable, onExport: (onDone: (String) -> Unit) -> Unit) {
    val c = colors
    var status by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(c.bg).padding(GUTTER), verticalArrangement = Arrangement.spacedBy(Space.m, androidx.compose.ui.Alignment.CenterVertically)) {
        Txt("We couldn’t open your data", Type.title)
        Txt("An update failed to prepare the database. Your data hasn’t been deleted. Export a copy to be safe, then try updating the app again.")
        Txt(error.javaClass.simpleName, Type.caption, c.inkMuted, maxLines = 4)
        Box(Modifier.padding(top = Space.s).clip(RectangleShape).background(c.accent).tap { onExport { status = it } }.padding(horizontal = Space.xl, vertical = Space.m)) {
            Txt("Export your data", Type.secondaryStrong, c.onAccent)
        }
        status?.let { Txt(it, Type.secondary) }
    }
}
