package com.madebykunal.doon.ui.onboarding

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madebykunal.doon.R
import com.madebykunal.doon.state.Prefs
import com.madebykunal.doon.ui.components.Txt
import com.madebykunal.doon.ui.components.blockTaps
import com.madebykunal.doon.ui.components.tap
import com.madebykunal.doon.ui.theme.GUTTER
import com.madebykunal.doon.ui.theme.Space
import com.madebykunal.doon.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private var introPlayed = false
private const val ICON_PX = 432

@Composable
fun Intro() {
    var visible by remember { mutableStateOf(!introPlayed) }
    if (!visible) return
    val brand = colorResource(R.color.brand)
    val density = LocalDensity.current
    val iconSize = if (Build.VERSION.SDK_INT >= 31) 288.dp else with(density) { ICON_PX.toDp() }
    val fade = remember { Animatable(1f) }
    val grow = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        introPlayed = true
        launch { grow.animateTo(1.18f, tween(420, easing = FastOutSlowInEasing)) }
        fade.animateTo(0f, tween(420, delayMillis = 60, easing = FastOutSlowInEasing))
        visible = false
    }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }.background(brand).blockTaps(), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(iconSize).graphicsLayer { scaleX = grow.value; scaleY = grow.value },
        )
    }
}

@Composable
fun Onboarding() {
    val brand = colorResource(R.color.brand)
    val scope = rememberCoroutineScope()
    val parts = remember { List(4) { Animatable(0f) } }
    val leave = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(380)
        parts.forEachIndexed { i, a -> launch { a.animateTo(1f, tween(520, delayMillis = 110 * i, easing = FastOutSlowInEasing)) } }
    }
    fun Modifier.rise(i: Int) = graphicsLayer {
        alpha = parts[i].value
        translationY = (1f - parts[i].value) * 28.dp.toPx()
    }
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { translationY = -leave.value * size.height }
            .background(brand).blockTaps()
            .statusBarsPadding().navigationBarsPadding().padding(GUTTER),
    ) {
        Column(Modifier.align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(132.dp).padding(bottom = Space.s).rise(0))
            Txt("DOON", Type.display.copy(fontSize = 72.sp, lineHeight = 74.sp), Color.White, Modifier.rise(1), unscaled = true)
            Txt("Make every day count.", Type.body.copy(fontSize = 22.sp, lineHeight = 28.sp), Color.White.copy(alpha = 0.85f), Modifier.rise(2))
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().rise(3)) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    .tap(pressedScale = 0.96f, haptic = true, label = "Begin") {
                        scope.launch {
                            leave.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
                            Prefs.setOnboarded(true)
                        }
                    }
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Txt("BEGIN", Type.nav.copy(fontSize = 22.sp, lineHeight = 26.sp), brand, unscaled = true)
            }
            Spacer(Modifier.padding(bottom = Space.s))
        }
    }
}
