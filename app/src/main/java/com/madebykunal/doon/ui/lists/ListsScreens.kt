package com.madebykunal.doon.ui.lists

import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.madebykunal.doon.data.Home
import com.madebykunal.doon.data.ListGroup
import com.madebykunal.doon.data.ListRepo
import com.madebykunal.doon.data.Live
import com.madebykunal.doon.data.T
import com.madebykunal.doon.data.TodoList
import com.madebykunal.doon.state.Nav
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.state.Route
import com.madebykunal.doon.state.SheetRoute
import com.madebykunal.doon.state.TodoActions
import com.madebykunal.doon.ui.components.EmojiTile
import com.madebykunal.doon.ui.components.HeaderButton
import com.madebykunal.doon.ui.components.Ic
import com.madebykunal.doon.ui.components.Icon
import com.madebykunal.doon.ui.components.ScreenHeader
import com.madebykunal.doon.ui.components.SettingsButton
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.dockInset
import com.madebykunal.doon.ui.components.hairlineBottom
import com.madebykunal.doon.ui.components.tap
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.HIT_TARGET
import com.madebykunal.doon.ui.theme.ROW_MIN_HEIGHT
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import com.madebykunal.doon.ui.todos.ItemList
import kotlin.math.roundToInt

@Composable
fun ListsScreen() {
    val groupsV = Live.version(T.GROUPS)
    val listsV = Live.version(T.LISTS)
    val todosV = Live.version(T.TODOS)
    val groups = remember(groupsV) { ListRepo.groups() }
    val lists = remember(listsV) { ListRepo.lists() }
    val counts = remember(todosV, listsV) { ListRepo.counts() }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Lists") {
            HeaderButton(Ic.Reorder, "Reorder folders", 22.dp) { Nav.openSheet(SheetRoute.ReorderFolders) }
            SettingsButton()
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            groups.forEachIndexed { i, group -> GroupSection(group, lists.filter { it.groupId == group.id }, counts, i % 2 == 0) }
            Spacer(Modifier.height(dockInset()))
        }
    }
}

@Composable
private fun GroupSection(group: ListGroup, lists: List<TodoList>, counts: Map<String, Int>, band: Boolean) {
    val c = colors
    var collapsed by remember(group.id) { mutableStateOf(group.collapsed) }
    Column(Modifier.fillMaxWidth().background(if (band) c.surface else c.bg).padding(top = Space.l, bottom = Space.m)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = GUTTER), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).heightIn(min = HIT_TARGET).tap(
                    label = if (collapsed) "Expand ${group.name}" else "Collapse ${group.name}",
                    onLongClick = { Nav.openSheet(SheetRoute.ListEditor(groupId = group.id)) },
                ) {
                    collapsed = !collapsed
                    ListRepo.updateGroup(group.id, "collapsed" to collapsed)
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt(group.name.uppercase(), Type.label, c.inkMuted, maxLines = 1, modifier = Modifier.weight(1f))
                if (collapsed && lists.isNotEmpty() && Prefs.listFolderCounts) CountText(lists.size)
            }
        }
        if (!collapsed) ReorderColumn(lists, counts, if (band) c.surface else c.bg)
    }
}

@Composable
private fun ReorderColumn(lists: List<TodoList>, counts: Map<String, Int>, bg: androidx.compose.ui.graphics.Color) {
    val rowPx = with(LocalDensity.current) { 60.dp.toPx() }
    var dragging by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val from = lists.indexOfFirst { it.id == dragging }
    val to = if (from < 0) -1 else (from + (offset / rowPx).roundToInt()).coerceIn(0, lists.size - 1)
    Column {
        lists.forEachIndexed { index, list ->
            val shift = when {
                from < 0 || list.id == dragging -> 0f
                index in (from + 1)..to -> -rowPx
                index in to until from -> rowPx
                else -> 0f
            }
            val animated by animateFloatAsState(shift, label = "shift")
            val isDragged = list.id == dragging
            Box(
                Modifier.zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragged) offset else animated }
                    .then(if (isDragged) Modifier.shadow(8.dp) else Modifier)
                    .pointerInput(list.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragging = list.id
                                offset = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                offset += amount.y
                            },
                            onDragEnd = {
                                val f = lists.indexOfFirst { it.id == list.id }
                                val t = (f + (offset / rowPx).roundToInt()).coerceIn(0, lists.size - 1)
                                if (f >= 0 && t != f) {
                                    val rest = lists.filter { it.id != list.id }
                                    ListRepo.reorderList(list.id, rest.getOrNull(t - 1)?.position, rest.getOrNull(t)?.position)
                                }
                                dragging = null
                                offset = 0f
                            },
                            onDragCancel = {
                                dragging = null
                                offset = 0f
                            },
                        )
                    },
            ) {
                ListRow(list, counts[list.id] ?: 0, bg)
            }
        }
    }
}

@Composable
private fun ListRow(list: TodoList, count: Int, bg: androidx.compose.ui.graphics.Color) {
    val c = colors
    Row(
        Modifier.fillMaxWidth().background(bg).tap(label = "${list.name}, $count open") { Nav.push(Route.ListDetail(list.id)) }
            .heightIn(min = 60.dp).padding(horizontal = GUTTER),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Txt(list.name.uppercase(), Type.listName, maxLines = 1, modifier = Modifier.weight(1f))
        if (count > 0 && Prefs.listCounts) CountText(count)
    }
}

@Composable
fun ListDetailScreen(id: String) {
    val version = Live.version(T.LISTS)
    val list = remember(version, id) { ListRepo.get(id) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(list?.name ?: "", back = true, onTitle = { Nav.openSheet(SheetRoute.ListEditor(listId = id)) })
        ItemList(Home.InList(id), "Today", TodoActions::moveToToday, "Nothing here yet. Tap anywhere to add.")
    }
}

@Composable
private fun CountText(n: Int) {
    val c = colors
    if (Prefs.listBigCounts) Txt(n.toString(), Type.display.copy(fontSize = 30.sp, lineHeight = 32.sp), c.accentText, maxLines = 1, tabularNums = true, unscaled = true)
    else Txt(n.toString(), Type.secondaryStrong, c.inkMuted, tabularNums = true)
}
