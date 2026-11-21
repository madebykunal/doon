package com.madebykunal.doon.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.state.SheetRoute
import com.madebykunal.doon.state.TodoActions
import com.madebykunal.doon.state.Ui
import com.madebykunal.doon.ui.components.DatePicker
import com.madebykunal.doon.ui.components.Ic
import com.madebykunal.doon.ui.components.Icon
import com.madebykunal.doon.ui.components.Odometer
import com.madebykunal.doon.ui.components.SearchButton
import com.madebykunal.doon.ui.components.SettingsButton
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.dockInset
import com.madebykunal.doon.ui.components.hairlineBottom
import com.madebykunal.doon.ui.components.hairlineTop
import com.madebykunal.doon.ui.components.tap
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.RADIUS_DOCK
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
private val DATE_ROW = 60.dp

@Composable
fun TodayScreen() {
    val statusTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            DayHeader(Ui.selectedDate)
            DayPager()
        }
        TodayPill()
        CalendarPanel(statusTop + Space.s + DATE_ROW + Space.xs)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.DayPager() {
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
    Box(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = Space.s)) {
        Column {
            Row(Modifier.fillMaxWidth().height(DATE_ROW).padding(horizontal = GUTTER), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).tap(label = if (Ui.calendarOpen) "Close calendar" else "Open calendar", onLongClick = openMenu) {
                        Editor.commitAll()
                        Ui.calendarOpen = !Ui.calendarOpen
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    Odometer(Dates.format(date, "dd"), Type.numeral)
                    Column {
                        Txt(Dates.format(date, "EEEE"), Type.h2, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                            Txt(Dates.format(date, "MMMM yyyy"), Type.secondary, c.inkMuted, maxLines = 1)
                            Icon(Ic.ChevronDown, c.inkMuted, 14.dp, modifier = Modifier.rotate(chevron))
                        }
                    }
                }
                SearchButton()
                SettingsButton()
            }
            Spacer(Modifier.height(Space.xs))
            if (Prefs.weekStrip) TopWeekStrip(date)
        }
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.BottomEnd) { ClearedStamp() }
    }
}

@Composable
private fun TopWeekStrip(selected: DateStr) {
    val c = colors
    val band = Prefs.weekBand
    var m = Modifier.fillMaxWidth()
    if (band) m = m.background(c.accentWash).hairlineTop(c.hairline).hairlineBottom(c.hairline)
    Box(m.padding(horizontal = Space.m, vertical = 6.dp)) { WeekStrip(selected) }
}

@Composable
private fun WeekStrip(selected: DateStr) {
    val c = colors
    val weekStartsOn = Prefs.weekStart
    val baseStart = remember(weekStartsOn) { Dates.startOfWeek(Ui.today, weekStartsOn) }
    fun startOf(page: Int) = Dates.addDays(baseStart, (page - CENTER) * 7)
    fun pageOf(date: DateStr) = CENTER + Math.floorDiv(Dates.diffDays(baseStart, date), 7)
    val pager = rememberPagerState(initialPage = pageOf(selected)) { PAGES }
    var visibleStart by remember { mutableStateOf(startOf(pager.currentPage)) }

    LaunchedEffect(selected, weekStartsOn) {
        val target = pageOf(selected)
        if (pager.currentPage != target) {
            if (abs(pager.currentPage - target) > 2) pager.scrollToPage(target) else pager.animateScrollToPage(target)
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect {
            visibleStart = startOf(it)
            RowDrag.visibleWeekStart = visibleStart
        }
    }
    LaunchedEffect(visibleStart) { SeriesRepo.materialize(Dates.addDays(visibleStart, -7), Dates.addDays(visibleStart, 13)) }
    val version = Live.version(T.TODOS)
    val openDates = remember(version, visibleStart) { TodoRepo.openDates(Dates.addDays(visibleStart, -7), Dates.addDays(visibleStart, 13)) }

    HorizontalPager(
        state = pager,
        modifier = Modifier.fillMaxWidth().height(62.dp).onGloballyPositioned { RowDrag.stripBounds = it.boundsInRoot() },
        key = { it },
        beyondViewportPageCount = 0,
    ) { page ->
        val start = startOf(page)
        WeekRow(Dates.weekDays(start), selected, openDates, c.accent)
    }
}

@Composable
private fun WeekRow(days: List<DateStr>, selected: DateStr, openDates: Set<String>, accent: Color) {
    val c = colors
    val today = Ui.today
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cell = maxWidth / 7
        val inset = (cell - 34.dp) / 2
        val index = days.indexOf(selected)
        if (index >= 0) {
            val x by animateDpAsState(cell * index + inset, tween(130), label = "pill")
            Box(Modifier.offset(x = x, y = 18.dp).size(34.dp).clip(CircleShape).background(accent))
        }
        val drop = RowDrag.dropDay?.let { days.indexOf(it) } ?: -1
        if (drop >= 0) {
            Box(Modifier.offset(x = cell * drop + inset, y = 18.dp).size(34.dp).graphicsLayer { scaleX = 1.15f; scaleY = 1.15f }.border(2.dp, c.accent, CircleShape))
        }
        Row(Modifier.fillMaxSize()) {
            days.forEach { date ->
                val isSelected = date == selected
                val isToday = date == today
                val lit = isSelected || isToday
                Column(Modifier.width(cell).fillMaxSize().tap { Ui.selectedDate = date }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                        Txt(Dates.format(date, "EEEEE"), Type.micro, if (lit) c.accentText else c.inkMuted)
                    }
                    Spacer(Modifier.height(2.dp))
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(if (isToday && !isSelected) c.accentTint else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Txt(
                            Dates.day(date).toString(),
                            if (lit) Type.secondaryStrong else Type.secondary,
                            if (isSelected) c.onAccent else if (isToday) c.accentText else c.ink,
                            tabularNums = true,
                        )
                    }
                    Box(Modifier.padding(top = 3.dp).size(4.dp).alpha(if (date in openDates) 0.7f else 0f).clip(CircleShape).background(if (isSelected) c.accent else c.inkMuted))
                }
            }
        }
    }
}

@Composable
private fun CalendarPanel(top: Dp) {
    val c = colors
    val open = Ui.calendarOpen
    AnimatedVisibility(open, enter = fadeIn(tween(130)), exit = fadeOut(tween(130))) {
        Box(Modifier.fillMaxSize().padding(top = top).background(Color.Black.copy(alpha = 0.32f)).tap(role = null, label = "Close calendar") { Ui.calendarOpen = false })
    }
    Box(Modifier.fillMaxWidth().padding(top = top)) {
        AnimatedVisibility(open, enter = expandVertically(tween(130), Alignment.Top) + fadeIn(tween(90)), exit = shrinkVertically(tween(130), Alignment.Top) + fadeOut(tween(130))) {
            var firstShown by remember { mutableStateOf(Dates.makeDate(Dates.year(Ui.selectedDate), Dates.month(Ui.selectedDate), 1)) }
            LaunchedEffect(firstShown) { SeriesRepo.materialize(Dates.addDays(firstShown, -7), Dates.addDays(firstShown, 42)) }
            val version = Live.version(T.TODOS)
            val openDates = remember(version, firstShown) { TodoRepo.openDates(Dates.addDays(firstShown, -7), Dates.addDays(firstShown, 42)) }
            var dragged by remember { mutableIntStateOf(0) }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = RADIUS_DOCK + Space.xs, bottomEnd = RADIUS_DOCK + Space.xs)).background(c.bg)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { dragged = 0 },
                            onDragEnd = { if (dragged < -24 * density) Ui.calendarOpen = false },
                        ) { _, amount -> dragged += amount.toInt() }
                    }
                    .padding(top = Space.xs, bottom = Space.s),
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
                Box(Modifier.fillMaxWidth().padding(top = Space.xs), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(36.dp, 4.dp).alpha(0.35f).clip(CircleShape).background(c.inkMuted))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayPill() {
    val c = colors
    val away = Ui.selectedDate != Ui.today && !Ui.calendarOpen && !WindowInsets.isImeVisible
    val bottom = dockInset() - Space.s
    Box(Modifier.fillMaxSize().padding(bottom = bottom), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(away, enter = fadeIn(tween(110)), exit = fadeOut(tween(110))) {
            Box(
                Modifier.shadow(3.dp, CircleShape).clip(CircleShape).background(c.surface).border(1.dp, c.hairline, CircleShape)
                    .tap(pressedAlpha = 0.7f, label = "Jump to today") { Ui.selectedDate = Ui.today }
                    .padding(horizontal = Space.l, vertical = Space.s),
            ) {
                Txt("Today", Type.captionStrong, c.accentText)
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
        Modifier.padding(end = Space.xxl, bottom = Space.m).graphicsLayer {
            this.alpha = alpha.value
            rotationZ = -8f
            scaleX = scale.value
            scaleY = scale.value
        }.border(2.5.dp, c.accentText, RoundedCornerShape(RADIUS_DOCK / 3)).padding(horizontal = Space.m, vertical = Space.xs),
    ) {
        Txt("Cleared ✓", Type.h2, c.accentText)
    }
}
