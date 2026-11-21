package com.madebykunal.doon.ui

import androidx.compose.ui.graphics.RectangleShape
import androidx.activity.compose.BackHandler
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
import com.madebykunal.doon.ui.todos.EditToolbar
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
                Route.Search -> SearchScreen()
                else -> Shell(imeVisible)
            }
        }
        EditToolbar()
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
                Box(Modifier.fillMaxSize().padding(Space.xl), contentAlignment = Alignment.BottomEnd) {
                    Box(
                        Modifier.size(56.dp).shadow(6.dp, RectangleShape).background(c.accent).tap(pressedAlpha = 0.8f, label = "Add", onClick = ::pressPlus),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Ic.Plus, c.onAccent, 26.dp, stroke = 2.5f) }
                }
            }
            SnackbarHost(if (imeVisible) TOOLBAR_HEIGHT + 8.dp else 92.dp)
        }
        if (!imeVisible) {
            if (onToday) WeekStripBar()
            BottomNav()
        }
    }
}

@Composable
private fun BottomNav() {
    val c = colors
    Row(Modifier.fillMaxWidth().background(c.accentDeep).navigationBarsPadding().height(64.dp)) {
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
                Txt(label.uppercase(), Type.nav, c.onAccentDeep.copy(alpha = if (selected) 1f else 0.62f), maxLines = 1, unscaled = true)
                Box(Modifier.padding(top = 5.dp).size(4.dp).background(if (selected) c.onAccentDeep else Color.Transparent))
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
                    fromTab != toTab || fromDepth == toDepth -> fadeIn(tween(160)) togetherWith fadeOut(tween(120))
                    toDepth > fromDepth -> slideInHorizontally(tween(220)) { it } togetherWith fadeOut(tween(220))
                    else -> fadeIn(tween(160)) togetherWith slideOutHorizontally(tween(200)) { it }
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
private fun SearchScreen() {
    val c = colors
    var term by remember { mutableStateOf("") }
    val query = term.trim()
    val todos = remember(query) { if (query.isEmpty()) emptyList() else TodoRepo.search(query) }
    val countdowns = remember(query) { if (query.isEmpty()) emptyList() else CountdownRepo.search(query) }
    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader("Search", back = true)
        Field(term, { term = it }, "To-dos, list items, countdowns", Modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(bottom = Space.s), autoFocus = true, imeAction = androidx.compose.ui.text.input.ImeAction.Search)
        LazyColumn(Modifier.fillMaxSize()) {
            if (query.isNotEmpty() && todos.isEmpty() && countdowns.isEmpty()) {
                item { Txt("Nothing matches “$query”.", Type.secondary, c.inkMuted, Modifier.fillMaxWidth().padding(GUTTER), align = androidx.compose.ui.text.style.TextAlign.Center) }
            }
            if (countdowns.isNotEmpty()) item { SectionLabel("Countdowns") }
            items(countdowns, key = { "c" + it.id }) { item ->
                OptionRow(item.title, icon = { Txt(item.emoji) }) { Nav.open(2, Route.CountdownDetail(item.id)) }
            }
            if (todos.isNotEmpty()) item { SectionLabel("To-dos") }
            items(todos, key = { "t" + it.id }) { todo ->
                val detail = todo.date?.let { Dates.format(it, "EEE, d MMM yyyy") } ?: todo.listId?.let { ListRepo.get(it)?.name } ?: "List"
                OptionRow(Markdown.plainText(todo.text), detail) {
                    val date = todo.date
                    val listId = todo.listId
                    if (date != null) {
                        Ui.selectedDate = date
                        Nav.open(0)
                    } else if (listId != null) Nav.open(1, Route.ListDetail(listId))
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
