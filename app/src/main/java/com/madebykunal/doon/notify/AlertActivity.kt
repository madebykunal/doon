package com.madebykunal.doon.notify

import androidx.compose.ui.graphics.RectangleShape
import android.app.KeyguardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madebykunal.doon.DoonApp
import com.madebykunal.doon.MainActivity
import com.madebykunal.doon.SystemBars
import com.madebykunal.doon.data.CountdownRepo
import com.madebykunal.doon.data.TodoRepo
import com.madebykunal.doon.lib.CountdownMath
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.Markdown
import com.madebykunal.doon.state.TodoActions
import com.madebykunal.doon.ui.components.Ic
import com.madebykunal.doon.ui.components.Icon
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.tap
import com.madebykunal.doon.ui.theme.DoonTheme
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.HIT_TARGET
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import com.madebykunal.doon.ui.theme.colors

private const val SNOOZE_MS = 10 * 60_000L

class AlertActivity : ComponentActivity() {
    private var nid = 0
    private var locked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        render(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        render(intent)
    }

    private fun render(intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: return finish()
        val id = intent.getStringExtra("id") ?: return finish()
        nid = intent.getIntExtra("nid", 0)
        locked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        if (!DoonApp.ready) return finish()
        if (!locked) Reminders.dismiss(nid)
        val firedAt = Dates.formatMillis(System.currentTimeMillis(), "HH:mm")
        val reduced = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        setContent {
            DoonTheme {
                SystemBars()
                AlertScreen(kind, id, firedAt, locked, reduced, ::leave, ::seeCountdown)
            }
        }
    }

    private fun leave() {
        Reminders.dismiss(nid)
        finishAndRemoveTask()
    }

    private fun seeCountdown(id: String) {
        Reminders.dismiss(nid)
        startActivity(
            Intent(this, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse("doon-open://countdown/$id"))
                .putExtra("open", "countdown:$id").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finishAndRemoveTask()
    }
}

@Composable
private fun AlertScreen(kind: String, id: String, firedAt: String, locked: Boolean, reduced: Boolean, leave: () -> Unit, seeCountdown: (String) -> Unit) {
    val c = colors
    val isTodo = kind == "todo"
    val todo = remember { if (isTodo) TodoRepo.get(id) else null }
    val countdown = remember { if (isTodo) null else CountdownRepo.get(id) }
    val missing = if (isTodo) todo == null || todo.deletedAt != null else countdown == null || countdown.deletedAt != null
    LaunchedEffect(missing) { if (missing) leave() }
    BackHandler { leave() }
    if (missing) return

    val pulse = if (reduced) 0f else {
        val transition = rememberInfiniteTransition(label = "pulse")
        val p by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "p")
        p
    }
    val title = if (todo != null) Markdown.plainText(todo.text) else countdown!!.title
    val detail = countdown?.let { CountdownMath.display(it.dates, Dates.today()) }?.let { it.value + if (it.unit.isNotEmpty()) " ${it.unit}" else "" }

    Column(Modifier.fillMaxSize().background(c.accent).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = GUTTER, vertical = Space.xxl)) {
        Txt("${if (isTodo) "Reminder" else "Countdown"} · $firedAt", Type.secondaryStrong, c.onAccent, Modifier.fillMaxWidth(), align = TextAlign.Center)
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xl, Alignment.CenterVertically)) {
            Box(Modifier.padding(bottom = Space.l).size(136.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(136.dp).graphicsLayer {
                        alpha = 0.35f * (1 - pulse)
                        scaleX = 1 + pulse * 0.45f
                        scaleY = 1 + pulse * 0.45f
                    }.border(2.dp, c.onAccent, RectangleShape),
                )
                Box(Modifier.size(136.dp).clip(RectangleShape).background(Color.White.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    if (isTodo) Icon(Ic.Bell, c.onAccent, 56.dp, stroke = 1.75f)
                    else Txt(countdown!!.emoji, Type.title.copy(fontSize = 64.sp, lineHeight = 78.sp), unscaled = true)
                }
            }
            Txt(title, Type.title.copy(fontSize = 34.sp, lineHeight = 41.sp), c.onAccent, maxLines = 4, align = TextAlign.Center)
            if (detail != null) Txt(detail, Type.h2, c.onAccent, align = TextAlign.Center)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            if (isTodo) {
                Action("Done", true) {
                    if (todo?.completed == false) TodoActions.toggle(id)
                    leave()
                }
                Action("Snooze 10 min", false) {
                    Reminders.setTodoReminder(id, System.currentTimeMillis() + SNOOZE_MS)
                    leave()
                }
            }
            if (!isTodo && !locked) Action("See countdown", true) { seeCountdown(id) }
            Action("Dismiss", !isTodo && locked, leave)
        }
        Spacer(Modifier)
    }
}

@Composable
private fun Action(label: String, filled: Boolean, onClick: () -> Unit) {
    val c = colors
    val m = if (filled) Modifier.background(c.onAccent, RectangleShape) else Modifier.border(1.5.dp, c.onAccent, RectangleShape)
    Box(
        Modifier.fillMaxWidth().heightIn(min = HIT_TARGET + 12.dp).clip(RectangleShape).then(m).tap(pressedAlpha = 0.75f, label = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Txt(label, Type.bodyStrong, if (filled) c.accent else c.onAccent) }
}
