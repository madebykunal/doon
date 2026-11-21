package com.madebykunal.doon.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madebykunal.doon.data.Home
import com.madebykunal.doon.data.Live
import com.madebykunal.doon.data.SeriesRepo
import com.madebykunal.doon.data.T
import com.madebykunal.doon.data.TodoRepo
import com.madebykunal.doon.lib.DateStr
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.state.DialogAction
import com.madebykunal.doon.state.Dialogs
import com.madebykunal.doon.state.Editor
import com.madebykunal.doon.state.Nav
import com.madebykunal.doon.state.SheetRoute
import com.madebykunal.doon.state.TodoActions
import com.madebykunal.doon.state.Ui
import com.madebykunal.doon.ui.components.DatePicker
import com.madebykunal.doon.ui.components.Ic
import com.madebykunal.doon.ui.components.Icon
import com.madebykunal.doon.ui.components.SearchButton
import com.madebykunal.doon.ui.components.SettingsButton
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.tap
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors
import com.madebykunal.doon.ui.todos.ItemList
import com.madebykunal.doon.ui.todos.RowDrag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val PAGES = 200_000
private const val CENTER = PAGES / 2
private val HEADER = 64.dp
private const val STRIP_CELLS = 5

@Composable
fun TodayScreen() {
    val statusTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            DayHeader(Ui.selectedDate)
            DayPager()
        }
        TodayPill()
        CalendarPanel(statusTop + HEADER)
    }
}

@Composable
private fun ColumnScope.DayPager() {
    val anchor = remember { Dates.dayNumber(Ui.selectedDate) }
    fun dateOf(page: Int) = Dates.fromDayNumber(anchor + (page - CENTER))
    fun pageOf(date: DateStr) = CENTER + (Dates.dayNumber(date) - anchor).toInt()
    val pager = rememberPagerState(initialPage = pageOf(Ui.selectedDate)) { PAGES }
    val selected = Ui.selectedDate

    LaunchedEffect(selected) {
        val target = pageOf(selected)
        if (pager.currentPage == target && !pager.isScrollInProgress) return@LaunchedEffect
        if (abs(pager.currentPage - target) > 1) pager.scrollToPage(target) else pager.animateScrollToPage(target)
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            val date = dateOf(page)
            if (date != Ui.selectedDate) Ui.selectedDate = date
        }
    }

    val enabled = Editor.editingId == null && Editor.composer == null && !Ui.calendarOpen && RowDrag.draggingId == null
    HorizontalPager(
        state = pager,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        userScrollEnabled = enabled,
        key = { it },
    ) { page ->
        val date = dateOf(page)
        LaunchedEffect(date) { SeriesRepo.materialize(date, date) }
        ItemList(Home.Day(date), "Tomorrow", TodoActions::moveToTomorrow, "Nothing planned. Tap anywhere to add.")
    }
}

@Composable
private fun DayHeader(date: DateStr) {
    val c = colors
    val chevron by animateFloatAsState(if (Ui.calendarOpen) 180f else 0f, tween(130), label = "chevron")
    val openMenu = {
        Dialogs.show(
            Dates.format(date, "EEEE, d MMMM"),
            actions = listOf(
                DialogAction("Complete all") { TodoActions.completeAll(date) },
                DialogAction("Move unfinished to…") { Nav.openSheet(SheetRoute.MoveTo(fromDate = date)) },
                DialogAction("Cancel", "cancel"),
            ),
        )
    }
    Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().height(HEADER).padding(start = GUTTER, end = Space.s), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).tap(label = if (Ui.calendarOpen) "Close calendar" else "Open calendar", onLongClick = openMenu) {
                    Editor.commitAll()
                    Ui.calendarOpen = !Ui.calendarOpen
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Txt(Dates.format(date, "EEEE, d MMMM").uppercase(), Type.display, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                Icon(Ic.ChevronDown, c.inkMuted, 18.dp, modifier = Modifier.rotate(chevron))
            }
            SearchButton()
            SettingsButton()
        }
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.BottomEnd) { ClearedStamp() }
    }
}

@Composable
fun WeekStripBar() {
    val c = colors
    val selected = Ui.selectedDate
    val today = Ui.today
    val base = remember { Dates.dayNumber(today) }
    fun dateOf(i: Int) = Dates.fromDayNumber(base + (i - CENTER))
    fun indexOf(date: DateStr) = CENTER + (Dates.dayNumber(date) - base).toInt()
    val state = rememberLazyListState(initialFirstVisibleItemIndex = indexOf(selected) - 2)
    val fling = rememberSnapFlingBehavior(state)

    LaunchedEffect(selected) {
        val target = indexOf(selected) - 2
        if (abs(state.firstVisibleItemIndex - target) > 10) state.scrollToItem(target) else state.animateScrollToItem(target)
    }
    var first by remember { mutableStateOf(dateOf(state.firstVisibleItemIndex)) }
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            first = dateOf(index)
            RowDrag.stripFirstDay = first
            RowDrag.stripOffsetPx = offset.toFloat()
        }
    }
    val windowStart = Dates.addDays(first, -7)
    val windowEnd = Dates.addDays(first, 14)
    LaunchedEffect(windowStart) { SeriesRepo.materialize(windowStart, windowEnd) }
    val version = Live.version(T.TODOS)
    val openDates = remember(version, windowStart) { TodoRepo.openDates(windowStart, windowEnd) }

    BoxWithConstraints(
        Modifier.fillMaxWidth().height(76.dp).background(c.accent).onGloballyPositioned {
            RowDrag.stripBounds = it.boundsInRoot()
            RowDrag.stripCellPx = it.size.width / STRIP_CELLS.toFloat()
        },
    ) {
        val cell = maxWidth / STRIP_CELLS
        LazyRow(state = state, flingBehavior = fling, modifier = Modifier.fillMaxSize()) {
            items(PAGES, key = { it }) { i ->
                val date = dateOf(i)
                DayCell(date, cell, date == selected, date == today, date in openDates, date == RowDrag.dropDay)
            }
        }
    }
}

@Composable
private fun DayCell(date: DateStr, width: Dp, isSelected: Boolean, isToday: Boolean, hasOpen: Boolean, isDrop: Boolean) {
    val c = colors
    val ink = c.onAccent
    var m = Modifier.width(width).fillMaxHeight().padding(horizontal = 4.dp, vertical = 6.dp)
    if (isSelected) m = m.background(ink.copy(alpha = 0.18f))
    if (isDrop) m = m.border(2.dp, ink, RectangleShape)
    Column(
        m.tap(label = Dates.format(date, "EEEE d MMMM")) { Ui.selectedDate = date },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Txt(Dates.format(date, "dd/MM"), Type.micro.copy(fontSize = 11.sp), ink.copy(alpha = 0.8f), unscaled = true, tabularNums = true)
        Txt(Dates.format(date, "EEE").uppercase(), Type.nav.copy(fontSize = 22.sp, lineHeight = 26.sp), ink, unscaled = true)
        Row(Modifier.padding(top = 3.dp).height(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (isToday) Box(Modifier.size(14.dp, 3.dp).background(ink))
            if (hasOpen && !isToday) Box(Modifier.size(3.dp).background(ink.copy(alpha = 0.7f)))
        }
    }
}

@Composable
private fun CalendarPanel(top: Dp) {
    val c = colors
    val open = Ui.calendarOpen
    AnimatedVisibility(open, enter = fadeIn(tween(130)), exit = fadeOut(tween(130))) {
        Box(Modifier.fillMaxSize().padding(top = top).background(Color.Black.copy(alpha = 0.4f)).tap(role = null, label = "Close calendar") { Ui.calendarOpen = false })
    }
    Box(Modifier.fillMaxWidth().padding(top = top)) {
        AnimatedVisibility(open, enter = expandVertically(tween(130), Alignment.Top) + fadeIn(tween(90)), exit = shrinkVertically(tween(130), Alignment.Top) + fadeOut(tween(130))) {
            var firstShown by remember { mutableStateOf(Dates.makeDate(Dates.year(Ui.selectedDate), Dates.month(Ui.selectedDate), 1)) }
            LaunchedEffect(firstShown) { SeriesRepo.materialize(Dates.addDays(firstShown, -7), Dates.addDays(firstShown, 42)) }
            val version = Live.version(T.TODOS)
            val openDates = remember(version, firstShown) { TodoRepo.openDates(Dates.addDays(firstShown, -7), Dates.addDays(firstShown, 42)) }
            var dragged by remember { mutableIntStateOf(0) }
            Column(
                Modifier.fillMaxWidth().background(c.surface)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { dragged = 0 },
                            onDragEnd = { if (dragged < -24 * density) Ui.calendarOpen = false },
                        ) { _, amount -> dragged += amount.toInt() }
                    }
                    .padding(top = Space.xs, bottom = Space.m),
            ) {
                DatePicker(
                    value = Ui.selectedDate,
                    onChange = {
                        Ui.selectedDate = it
                        Ui.calendarOpen = false
                    },
                    openDates = openDates,
                    onMonthShown = { firstShown = it },
                    showToday = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayPill() {
    val c = colors
    val away = Ui.selectedDate != Ui.today && !Ui.calendarOpen && !WindowInsets.isImeVisible
    Box(Modifier.fillMaxSize().padding(bottom = Space.xl + 10.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(away, enter = fadeIn(tween(110)), exit = fadeOut(tween(110))) {
            Box(
                Modifier.shadow(3.dp, RectangleShape).background(c.surface).border(1.dp, c.hairline, RectangleShape)
                    .tap(pressedAlpha = 0.7f, label = "Jump to today") { Ui.selectedDate = Ui.today }
                    .padding(horizontal = Space.l, vertical = Space.s),
            ) {
                Txt("TODAY", Type.label, c.accentText)
            }
        }
    }
}

@Composable
private fun ClearedStamp() {
    val c = colors
    val cleared = Ui.cleared
    val seen = remember { mutableIntStateOf(cleared) }
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(cleared) {
        if (cleared == seen.intValue) return@LaunchedEffect
        seen.intValue = cleared
        scale.snapTo(1.6f)
        launch { scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f)) }
        alpha.animateTo(1f, tween(110))
        delay(1200)
        alpha.animateTo(0f, tween(160))
    }
    if (alpha.value == 0f) return
    Box(
        Modifier.padding(end = Space.xxl).graphicsLayer {
            this.alpha = alpha.value
            rotationZ = -8f
            scaleX = scale.value
            scaleY = scale.value
        }.border(2.5.dp, c.accentText, RectangleShape).padding(horizontal = Space.m, vertical = Space.xs),
    ) {
        Txt("CLEARED ✓", Type.nav, c.accentText)
    }
}
