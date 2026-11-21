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
import com.madebykunal.doon.ui.components.Ic
import com.madebykunal.doon.ui.components.Icon
import com.madebykunal.doon.ui.today.WeekStripBar
import com.madebykunal.doon.ui.todos.QuickAdd
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
        androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = if (overlay == null) !deepIsDark else !c.dark
    }
    Box(Modifier.fillMaxSize().background(c.bg)) {
        AnimatedContent(
            targetState = overlay,
            transitionSpec = {
                if (targetState != null) (slideInHorizontally(tween(220)) { it } togetherWith fadeOut(tween(220)))
                else (fadeIn(tween(160)) togetherWith slideOutHorizontally(tween(200)) { it })
            },
            label = "overlay",
        ) { route ->
            when (route) {
                Route.Settings -> SettingsScreen()
                else -> Shell(imeVisible)
            }
        }
        DragGhost()
        QuickAdd()
        SheetHost { SheetContent(it) }
        DialogHost()
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
            if (onToday && !Prefs.weekStripTop) WeekStripBar()
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
    BoxWithConstraints(Modifier.fillMaxSize().padding(Space.l), contentAlignment = Alignment.BottomEnd) {
        val size = 60.dp
        val maxX = with(density) { (maxWidth - size).toPx() }
        val maxY = with(density) { (maxHeight - size).toPx() }
        val snap = with(density) { 96.dp.toPx() }
        val home = fabOffset.value.getDistance() < snap
        Box(
            Modifier.offset { IntOffset(fabOffset.value.x.roundToInt(), fabOffset.value.y.roundToInt()) }
                .size(size)
                .shadow(if (home) 6.dp else 10.dp, RectangleShape)
                .background(c.accent)
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (fabOffset.value.getDistance() < snap) fabOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.7f, stiffness = 400f))
                            }
                        },
                    ) { change, amount ->
                        change.consume()
                        val next = fabOffset.value + amount
                        scope.launch { fabOffset.snapTo(Offset(next.x.coerceIn(-maxX, 0f), next.y.coerceIn(-maxY, 0f))) }
                    }
                }
                .tap(pressedAlpha = 0.8f, label = "Add", onClick = ::pressPlus),
            contentAlignment = Alignment.Center,
        ) { Icon(Ic.Plus, c.onAccent, 26.dp, stroke = 2.5f) }
    }
}

@Composable
private fun BottomNav() {
    val c = colors
    Row(Modifier.fillMaxWidth().background(c.accentDeep).navigationBarsPadding().height(80.dp)) {
        TAB_LABELS.forEachIndexed { index, label ->
            val selected = Nav.tab == index
            Column(
                Modifier.weight(1f).fillMaxHeight().tap(label = label) {
                    if (Nav.tab == index) Nav.stacks[index].clear()
                    Nav.selectTab(index)
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Txt(label.uppercase(), Type.nav, c.onAccentDeep, maxLines = 1, unscaled = true)
                val dot by animateFloatAsState(if (selected) 1f else 0f, tween(200), label = "dot")
                Box(Modifier.padding(top = 4.dp).size(5.dp).graphicsLayer { scaleX = dot; scaleY = dot; alpha = dot }.background(c.onAccentDeep, CircleShape))
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
                }
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
