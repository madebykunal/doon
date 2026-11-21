package com.madebykunal.doon.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.madebykunal.doon.ui.components.Dock
import com.madebykunal.doon.ui.components.Field
import com.madebykunal.doon.ui.components.OptionRow
import com.madebykunal.doon.ui.components.ScreenHeader
import com.madebykunal.doon.ui.components.SectionLabel
import com.madebykunal.doon.ui.components.SheetHost
import com.madebykunal.doon.ui.components.SnackbarHost
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.dockInset
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
        else -> when (Nav.tab) {
            0 -> Editor.openComposer(Home.Day(Ui.selectedDate), Where.Bottom)
            1 -> Nav.openSheet(SheetRoute.ListEditor(mode = "list"))
            2 -> Nav.openSheet(SheetRoute.CountdownEditor())
            else -> Nav.openSheet(SheetRoute.HabitEditor())
        }
    }
}

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
                else -> Tabs()
            }
        }
        if (overlay == null) Dock(!imeVisible, ::pressPlus)
        EditToolbar()
        DragGhost()
        SheetHost { SheetContent(it) }
        SnackbarHost(if (imeVisible) TOOLBAR_HEIGHT + 8.dp else dockInset())
        DialogHost()
    }
}

@Composable
private fun Tabs() {
    val holder = rememberSaveableStateHolder()
    val tab = Nav.tab
    val route = Nav.stacks[tab].lastOrNull()
    Box(Modifier.fillMaxSize().imePadding()) {
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
        Box(Modifier.padding(top = Space.s).clip(CircleShape).background(c.accent).tap { onExport { status = it } }.padding(horizontal = Space.xl, vertical = Space.m)) {
            Txt("Export your data", Type.secondaryStrong, c.onAccent)
        }
        status?.let { Txt(it, Type.secondary) }
    }
}
