package com.madebykunal.doon.state

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.madebykunal.doon.data.Home
import com.madebykunal.doon.data.Where
import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.Dates

object Ui {
    var today by mutableStateOf(Dates.today()); private set
    var selectedDate by mutableStateOf(today)
    var calendarOpen by mutableStateOf(false)
    var cleared by mutableIntStateOf(0)
    var resumes by mutableIntStateOf(0)

    fun refreshToday(): Boolean {
        val now = Dates.today()
        if (now == today) return false
        val wasOnToday = selectedDate == today
        today = now
        if (wasOnToday) selectedDate = now
        return true
    }
}

sealed interface Route {
    data class ListDetail(val id: String) : Route
    data class CountdownDetail(val id: String) : Route
    data class HabitDetail(val id: String) : Route
    data object Settings : Route
    data object Search : Route
}

sealed interface SheetRoute {
    data class TodoDetails(val id: String) : SheetRoute
    data class MoveTo(val id: String? = null, val fromDate: DateStr? = null) : SheetRoute
    data class ListEditor(val listId: String? = null, val groupId: String? = null, val mode: String? = null) : SheetRoute
    data class CountdownEditor(val id: String? = null) : SheetRoute
    data class HabitEditor(val id: String? = null) : SheetRoute
    class Emoji(val onPick: (String) -> Unit) : SheetRoute
}

class SheetEntry(val route: SheetRoute, val key: Int) {
    val visible = MutableTransitionState(false).apply { targetState = true }
}

object Nav {
    var tab by mutableIntStateOf(0); private set
    val stacks = List(4) { mutableStateListOf<Route>() }
    val overlay = mutableStateListOf<Route>()
    val sheets = mutableStateListOf<SheetEntry>()
    private var sheetKeys = 0

    fun selectTab(index: Int) {
        if (index != 0) Ui.calendarOpen = false
        tab = index
    }

    fun push(route: Route) {
        when (route) {
            Route.Settings, Route.Search -> overlay.add(route)
            is Route.ListDetail -> stacks[tab].add(route)
            is Route.CountdownDetail -> stacks[tab].add(route)
            is Route.HabitDetail -> stacks[tab].add(route)
        }
    }

    fun open(tabIndex: Int, route: Route? = null) {
        overlay.clear()
        closeAllSheets()
        selectTab(tabIndex)
        stacks[tabIndex].clear()
        if (route != null) stacks[tabIndex].add(route)
    }

    fun popScreen() {
        if (overlay.isNotEmpty()) overlay.removeAt(overlay.size - 1)
        else stacks[tab].removeLastOrNull()
    }

    fun openSheet(route: SheetRoute) {
        Editor.commitAll()
        sheets.add(SheetEntry(route, ++sheetKeys))
    }

    fun closeSheet() {
        sheets.lastOrNull { it.visible.targetState }?.visible?.targetState = false
    }

    fun closeAllSheets() {
        sheets.forEach { it.visible.targetState = false }
    }

    fun removeSheet(entry: SheetEntry) {
        sheets.remove(entry)
    }

    val currentRoute: Route? get() = overlay.lastOrNull() ?: stacks[tab].lastOrNull()

    fun canGoBack(): Boolean =
        Dialogs.current != null || sheets.any { it.visible.targetState } || Ui.calendarOpen ||
            Editor.editingId != null || Editor.composer != null || overlay.isNotEmpty() || stacks[tab].isNotEmpty()

    fun back() {
        when {
            Dialogs.current != null -> Dialogs.dismiss()
            sheets.any { it.visible.targetState } -> closeSheet()
            Ui.calendarOpen -> Ui.calendarOpen = false
            Editor.editingId != null || Editor.composer != null -> Editor.commitAll()
            else -> popScreen()
        }
    }
}

data class ComposerState(val home: Home, val where: Where)

object Editor {
    var editingId by mutableStateOf<String?>(null); private set
    var composer by mutableStateOf<ComposerState?>(null)
    var draftText = ""
    var draftDescription = ""
    var keepWords = false

    fun stopEditing(save: Boolean = true) {
        val id = editingId ?: return
        editingId = null
        if (save) TodoActions.saveEdit(id, draftText)
    }

    fun closeComposer(save: Boolean) {
        val open = composer ?: return
        composer = null
        if (save) TodoActions.addFromInput(draftText, open.home, open.where, keepWords, draftDescription)
    }

    fun commitAll() {
        stopEditing()
        closeComposer(true)
    }

    fun startEditing(id: String, text: String) {
        commitAll()
        draftText = text
        editingId = id
    }

    fun openComposer(home: Home, where: Where) {
        commitAll()
        draftText = ""
        draftDescription = ""
        keepWords = false
        composer = ComposerState(home, where)
    }
}

data class DialogAction(val label: String, val tone: String = "default", val onPress: (() -> Unit)? = null)

data class DialogSpec(val title: String, val message: String? = null, val actions: List<DialogAction>)

object Dialogs {
    var current by mutableStateOf<DialogSpec?>(null); private set

    fun show(title: String, message: String? = null, actions: List<DialogAction>) {
        current = DialogSpec(title, message, actions)
    }

    fun dismiss() {
        current = null
    }
}

data class SnackSpec(val id: Int, val message: String, val onUndo: (() -> Unit)?, val undoUntil: Long)

object Snack {
    const val UNDO_WINDOW_MS = 5000L
    private var nextId = 1
    var current by mutableStateOf<SnackSpec?>(null); private set

    fun show(message: String, onUndo: (() -> Unit)? = null) {
        current = SnackSpec(nextId++, message, onUndo, System.currentTimeMillis() + UNDO_WINDOW_MS)
    }

    fun dismiss(id: Int) {
        if (current?.id == id) current = null
    }
}
