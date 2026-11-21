package com.madebykunal.doon.ui.todos

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.madebykunal.doon.data.Home
import com.madebykunal.doon.data.Live
import com.madebykunal.doon.data.T
import com.madebykunal.doon.data.Todo
import com.madebykunal.doon.data.TodoRepo
import com.madebykunal.doon.data.Where
import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.Markdown
import com.madebykunal.doon.state.Editor
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.state.TodoActions
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.dockInset
import com.madebykunal.doon.ui.theme.TOOLBAR_HEIGHT
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import kotlinx.coroutines.launch

data class DisplayItem(val todo: Todo, val count: Int)

fun arrange(items: List<Todo>, hideCompleted: Boolean, sinkCompleted: Boolean): List<DisplayItem> {
    val shown = mutableListOf<DisplayItem>()
    val sunk = mutableListOf<DisplayItem>()
    var headingIndex = -1
    var headingCount = 0
    fun closeHeading() {
        if (headingIndex >= 0) shown[headingIndex] = shown[headingIndex].copy(count = headingCount)
    }
    for (todo in items) {
        if (todo.isHeading) {
            closeHeading()
            shown.add(DisplayItem(todo, 0))
            headingIndex = shown.size - 1
            headingCount = 0
            continue
        }
        val done = todo.completed
        if (!done && headingIndex >= 0) headingCount++
        if (done && hideCompleted) continue
        val entry = DisplayItem(todo, 0)
        if (done && sinkCompleted) sunk.add(entry)
        else if (headingIndex < 0 || !shown[headingIndex].todo.collapsed) shown.add(entry)
    }
    closeHeading()
    return shown + sunk
}

object RowDrag {
    var draggingId by mutableStateOf<String?>(null)
    var ghostText by mutableStateOf("")
    var ghostX by mutableFloatStateOf(0f)
    var ghostY by mutableFloatStateOf(0f)
    var ghostSize by mutableStateOf(IntSize.Zero)
    var dropDay by mutableStateOf<DateStr?>(null)
    var stripFirstDay by mutableStateOf(Dates.today())
    var stripOffsetPx = 0f
    var stripCellPx = 1f
    var stripBounds: Rect? = null

    fun hitDay(root: Offset): DateStr? {
        val b = stripBounds ?: return null
        if (!b.contains(root)) return null
        val index = ((root.x - b.left + stripOffsetPx) / stripCellPx).toInt()
        return Dates.addDays(stripFirstDay, index)
    }
}

private sealed interface Entry {
    val key: String

    data class RowItem(val item: DisplayItem) : Entry {
        override val key get() = item.todo.id
    }

    data object EmptyItem : Entry {
        override val key = "__empty"
    }
}

private const val PULL_LIMIT = 140f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemList(home: Home, rightLabel: String, onSwipeRight: (String) -> Unit, emptyText: String) {
    val c = colors
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val version = Live.version(T.TODOS)
    val hide = false
    val sink = false
    val display = remember(version, home, hide, sink) { arrange(TodoRepo.items(home), hide, sink) }
    var dragOrder by remember(home) { mutableStateOf<List<DisplayItem>?>(null) }
    val shown = dragOrder ?: display
    val latest by rememberUpdatedState(display)
    val state = rememberLazyListState()
    val onDay = home is Home.Day

    val entries = buildList {
        shown.forEach { add(Entry.RowItem(it)) }
        if (shown.isEmpty()) add(Entry.EmptyItem)
    }

    var pull by remember { mutableFloatStateOf(0f) }
    var travel by remember { mutableFloatStateOf(0f) }
    val threshold = with(density) { 64.dp.toPx() }
    val limit = with(density) { PULL_LIMIT.dp.toPx() }
    fun rubber(d: Float) = limit * (1 - 1 / (d / limit + 1))
    val pullConnection = remember(home) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (travel > 0f && available.y < 0f) {
                    val next = maxOf(0f, travel + available.y)
                    val used = next - travel
                    travel = next
                    pull = rubber(travel)
                    return Offset(0f, used)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f && RowDrag.draggingId == null) {
                    travel += available.y
                    pull = rubber(travel)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (travel <= 0f) return Velocity.Zero
                val armed = pull >= threshold
                travel = 0f
                if (armed) Editor.openComposer(home, Where.Top)
                animate(pull, 0f, animationSpec = spring(dampingRatio = 0.9f, stiffness = 520f)) { v, _ -> pull = v }
                return available
            }
        }
    }

    var listCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragKey by remember { mutableStateOf<String?>(null) }
    var grabY by remember { mutableFloatStateOf(0f) }
    var finger by remember { mutableStateOf(Offset.Zero) }
    var startFinger by remember { mutableStateOf(Offset.Zero) }

    fun info(key: String): LazyListItemInfo? = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    fun updateGhost() {
        val coords = listCoords ?: return
        val root = coords.localToRoot(finger)
        val origin = coords.localToRoot(Offset.Zero)
        RowDrag.ghostX = origin.x + (finger.x - startFinger.x)
        RowDrag.ghostY = root.y - grabY
        RowDrag.dropDay = if (onDay) RowDrag.hitDay(root) else null
    }

    fun checkReorder() {
        val key = dragKey ?: return
        if (RowDrag.dropDay != null) return
        val order = dragOrder ?: return
        val own = info(key) ?: return
        val middle = finger.y - grabY + own.size / 2f
        val target = state.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key != key && it.key is String && (it.key as String).let { k -> !k.startsWith("__") } &&
                middle.toInt() in it.offset..(it.offset + it.size)
        } ?: return
        val from = order.indexOfFirst { it.todo.id == key }
        val to = order.indexOfFirst { it.todo.id == target.key }
        if (from < 0 || to < 0 || from == to) return
        val next = order.toMutableList()
        next.add(to, next.removeAt(from))
        dragOrder = next
        if (from == 0 || to == 0) scope.launch { state.scrollToItem(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) }
    }

    fun finishDrag(commit: Boolean) {
        val key = dragKey
        val order = dragOrder
        val day = RowDrag.dropDay
        dragKey = null
        RowDrag.draggingId = null
        RowDrag.dropDay = null
        if (key != null && commit) {
            if (day != null && home is Home.Day && day != home.date) {
                dragOrder = null
                TodoActions.moveTo(key, Home.Day(day))
                return
            }
            if (order != null && order.map { it.todo.id } != latest.map { it.todo.id }) {
                val to = order.indexOfFirst { it.todo.id == key }
                val before = order.filter { it.todo.id != key }.getOrNull(to - 1)?.todo
                val stored = TodoRepo.items(home).filter { it.id != key }
                val beforeAt = if (before != null) stored.indexOfFirst { it.id == before.id } else -1
                TodoRepo.reorder(key, stored.getOrNull(beforeAt)?.position, stored.getOrNull(beforeAt + 1)?.position)
            }
        }
        dragOrder = null
    }

    LaunchedEffect(dragKey) {
        if (dragKey == null) return@LaunchedEffect
        val zone = with(density) { 72.dp.toPx() }
        val speed = with(density) { 3.dp.toPx() }
        while (dragKey != null) {
            withFrameNanos { }
            if (RowDrag.dropDay != null) continue
            val height = state.layoutInfo.viewportSize.height
            val y = finger.y
            val delta = when {
                y < zone -> -speed
                y > height - zone -> speed
                else -> 0f
            }
            if (delta != 0f) {
                val moved = state.scrollBy(delta)
                if (moved != 0f) checkReorder()
            }
        }
    }

    val imeVisible = WindowInsets.isImeVisible
    val bottom = if (imeVisible) TOOLBAR_HEIGHT + 16.dp else dockInset()

    Box(Modifier.fillMaxSize().nestedScroll(pullConnection)) {
        if (pull > 0f) {
            Box(Modifier.fillMaxWidth().padding(top = 16.dp).alpha(if (pull >= threshold) 1f else minOf(1f, pull / threshold)), contentAlignment = Alignment.Center) {
                Txt(if (pull >= threshold) "Release to add" else "Pull to add", Type.captionStrong, c.inkMuted)
            }
        }
        LazyColumn(
            state = state,
            userScrollEnabled = dragKey == null,
            contentPadding = PaddingValues(bottom = bottom),
            modifier = Modifier.fillMaxSize()
                .graphicsLayer { translationY = pull }
                .onGloballyPositioned { listCoords = it }
                .pointerInput(home) {
                    detectTapGestures { pos ->
                        val last = state.layoutInfo.visibleItemsInfo.lastOrNull()
                        val end = when {
                            last == null -> 0
                            last.key == Entry.EmptyItem.key -> last.offset
                            else -> last.offset + last.size
                        }
                        if (pos.y <= end) return@detectTapGestures
                        if (Editor.editingId != null || Editor.composer != null) Editor.commitAll()
                        else Editor.openComposer(home, Where.Bottom)
                    }
                }
                .pointerInput(home) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val hit = state.layoutInfo.visibleItemsInfo.firstOrNull {
                                offset.y.toInt() in it.offset..(it.offset + it.size) && (it.key as? String)?.startsWith("__") == false
                            } ?: return@detectDragGesturesAfterLongPress
                            val id = hit.key as String
                            val item = latest.firstOrNull { it.todo.id == id } ?: return@detectDragGesturesAfterLongPress
                            Editor.stopEditing()
                            dragOrder = latest
                            dragKey = id
                            grabY = offset.y - hit.offset
                            finger = offset
                            startFinger = offset
                            RowDrag.ghostText = Markdown.plainText(item.todo.text)
                            RowDrag.ghostSize = IntSize(size.width, hit.size)
                            RowDrag.draggingId = id
                            updateGhost()
                        },
                        onDrag = { change, amount ->
                            if (dragKey == null) return@detectDragGesturesAfterLongPress
                            change.consume()
                            finger += amount
                            updateGhost()
                            checkReorder()
                        },
                        onDragEnd = { finishDrag(true) },
                        onDragCancel = { finishDrag(false) },
                    )
                },
        ) {
            items(entries, key = { it.key }, contentType = { it::class }) { entry ->
                when (entry) {
                    is Entry.RowItem -> TodoRow(
                        entry.item.todo,
                        entry.item.count,
                        rightLabel,
                        onSwipeRight,
                        Modifier.animateItem(fadeInSpec = null).alpha(if (dragKey == entry.key) 0f else 1f),
                    )
                    Entry.EmptyItem -> Txt(emptyText, Type.secondary, c.inkMuted, Modifier.fillMaxWidth().padding(top = 48.dp), align = TextAlign.Center)
                }
            }
        }
    }
}
