package com.madebykunal.doon.ui.todos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.madebykunal.doon.data.Home
import com.madebykunal.doon.data.ListRepo
import com.madebykunal.doon.data.Where
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.SmartInput
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.madebykunal.doon.state.ComposerState
import com.madebykunal.doon.state.Editor
import com.madebykunal.doon.state.TodoActions
import com.madebykunal.doon.state.Ui
import com.madebykunal.doon.ui.components.DatePicker
import com.madebykunal.doon.ui.components.Ic
import com.madebykunal.doon.ui.components.Icon
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.blockTaps
import com.madebykunal.doon.ui.components.tap
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors

@Composable
fun QuickAdd() {
    val open = Editor.composer
    var last by remember { mutableStateOf(open) }
    if (open != null) last = open
    AnimatedVisibility(open != null, enter = fadeIn(tween(140)), exit = fadeOut(tween(140))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).tap(role = null, label = "Close") { Editor.closeComposer(true) })
    }
    Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(open != null, enter = slideInVertically(tween(200)) { it }, exit = slideOutVertically(tween(160)) { it }) {
            val state = last ?: return@AnimatedVisibility
            Panel(state)
        }
    }
}

@Composable
private fun Panel(state: ComposerState) {
    val c = colors
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var ignoreRepeat by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val home = state.home
    val smart = remember(value, ignoreRepeat) { if (ignoreRepeat || value.isBlank()) null else SmartInput.parse(value, Ui.today).takeIf { it.found } }
    val highlight = remember(smart, c) {
        val spans = smart?.spans.orEmpty()
        VisualTransformation { text ->
            val styled = buildAnnotatedString {
                append(text)
                spans.filter { it.last < text.length }.forEach { addStyle(SpanStyle(color = c.accentText, background = c.accentTint), it.first, it.last + 1) }
            }
            TransformedText(styled, OffsetMapping.Identity)
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    fun change(next: String) {
        value = next
        Editor.draftText = next
        if (next.isEmpty()) {
            ignoreRepeat = false
            Editor.keepWords = false
        }
    }

    fun submit() {
        val text = value.trim()
        if (text.isEmpty()) {
            Editor.closeComposer(false)
            return
        }
        val created = TodoActions.addFromInput(text, home, state.where, ignoreRepeat, note)
        change("")
        note = ""
        Editor.draftDescription = ""
        if (state.where != Where.Bottom && created != null) Editor.composer = ComposerState(home, Where.After(created.id))
        runCatching { focus.requestFocus() }
    }

    val place = when (home) {
        is Home.Day -> when (home.date) {
            Ui.today -> "Today"
            Dates.addDays(Ui.today, 1) -> "Tomorrow"
            else -> Dates.format(home.date, "EEE d MMM")
        }
        is Home.InList -> ListRepo.get(home.listId)?.name ?: "List"
    }

    Column(Modifier.fillMaxWidth().background(c.surface).blockTaps().navigationBarsPadding().padding(top = Space.l, bottom = Space.m)) {
        BasicTextField(
            value = value,
            onValueChange = { next ->
                if (next.contains('\n')) {
                    change(next.replace("\n", ""))
                    submit()
                } else change(next)
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = GUTTER).focusRequester(focus),
            textStyle = Type.h2.copy(color = c.ink),
            cursorBrush = SolidColor(c.accent),
            visualTransformation = highlight,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Txt("e.g. Call mum on fri at 6pm", Type.h2, c.inkMuted)
                    inner()
                }
            },
        )
        BasicTextField(
            value = note,
            onValueChange = {
                note = it.take(2000)
                Editor.draftDescription = note
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = GUTTER).padding(top = Space.s),
            textStyle = Type.secondary.copy(color = c.ink),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            decorationBox = { inner ->
                Box {
                    if (note.isEmpty()) Txt("Description", Type.secondary, c.inkMuted)
                    inner()
                }
            },
        )
        if (picking && home is Home.Day) {
            Box(Modifier.padding(top = Space.s)) {
                DatePicker(home.date, {
                    Editor.composer = ComposerState(Home.Day(it), Where.Bottom)
                    picking = false
                })
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = GUTTER, end = Space.m, top = Space.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Row(
                Modifier.height(40.dp).background(c.hairline).tap(enabled = home is Home.Day, label = "Change day") { picking = !picking }.padding(horizontal = Space.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Ic.Calendar, c.inkMuted, 16.dp)
                Txt(place, Type.secondaryStrong, maxLines = 1)
            }
            if (smart != null) {
                Row(
                    Modifier.height(40.dp).weight(1f, fill = false).background(c.accentTint).tap(label = "Keep my words as typed") {
                        ignoreRepeat = true
                        Editor.keepWords = true
                    }.padding(horizontal = Space.m),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(if (smart.rule != null) Ic.Repeat else Ic.Bell, c.accentText, 14.dp)
                    Txt(smart.summary(Ui.today), Type.captionStrong, c.accentText, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    Txt("×", Type.caption, c.accentText)
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(40.dp).background(if (value.isBlank()) c.hairline else c.accent).tap(label = "Add") { submit() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Ic.ArrowRight, if (value.isBlank()) c.inkMuted else c.onAccent, 18.dp, stroke = 2.25f)
            }
        }
    }
}
