package com.madebykunal.doon.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madebykunal.doon.R
import com.madebykunal.doon.lib.ColorMath
import com.madebykunal.doon.state.Prefs

object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

val GUTTER = 20.dp
val ROW_MIN_HEIGHT = 52.dp
val HIT_TARGET = 44.dp
val CHECKBOX_SIZE = 22.dp
val RADIUS_CARD = 16.dp
val RADIUS_DOCK = 24.dp
val DOCK_HEIGHT = 60.dp
val DOCK_SIDE = 16.dp
val TOOLBAR_HEIGHT = 52.dp

object Accents {
    val presets = linkedMapOf(
        "ink" to ("Ink" to "#2B2B30"),
        "tomato" to ("Tomato" to "#E5484D"),
        "coral" to ("Coral" to "#F2765A"),
        "marigold" to ("Marigold" to "#E9A23B"),
        "sage" to ("Sage" to "#5B9A6F"),
        "lagoon" to ("Lagoon" to "#1E9E9A"),
        "cobalt" to ("Cobalt" to "#3B5BDB"),
        "iris" to ("Iris" to "#6E56CF"),
        "plum" to ("Plum" to "#A23E8C"),
        "moss" to ("Moss" to "#6B7A2E"),
    )
}

fun accentHex(): String = if (Prefs.accent == "custom") Prefs.customColor else Accents.presets[Prefs.accent]?.second ?: "#1E9E9A"

val highlightIds = listOf("yellow", "green", "blue", "pink")
private val highlightBars = mapOf("yellow" to "#E3B341", "green" to "#6DB585", "blue" to "#6C9EE0", "pink" to "#E88BB0")

fun hex(value: String): Color = Color(ColorMath.toArgb(value))

private class Base(val bg: String, val surface: String, val ink: String, val inkMuted: String, val hairline: Color)

private val LIGHT = Base("#FAF8F5", "#FFFFFF", "#17171A", "#6E6C67", Color(23, 23, 26, (0.07f * 255).toInt()))
private val DARK = Base("#0F0F11", "#18181B", "#F2F1EE", "#9A9894", Color(255, 255, 255, (0.08f * 255).toInt()))
private val BLACK = Base("#000000", "#0B0B0C", "#F2F1EE", "#9A9894", Color(255, 255, 255, (0.08f * 255).toInt()))

@Immutable
data class DoonColors(
    val kind: String,
    val bg: Color,
    val surface: Color,
    val ink: Color,
    val inkMuted: Color,
    val hairline: Color,
    val accent: Color,
    val accentTint: Color,
    val accentWash: Color,
    val accentText: Color,
    val onAccent: Color,
    val danger: Color,
    val dangerText: Color,
    val onDanger: Color,
    val bgHex: String,
) {
    val dark get() = kind != "light"

    fun highlightBar(id: String): Color = hex(highlightBars[id] ?: "#E3B341")

    fun highlightTint(id: String): Color =
        hex(ColorMath.mix(bgHex, highlightBars[id] ?: "#E3B341", if (kind == "light") 0.13 else 0.14))

    fun accentFor(hexValue: String): Pair<Color, Color> {
        val base = when (kind) {
            "dark" -> DARK
            "black" -> BLACK
            else -> LIGHT
        }
        val a = ColorMath.ensureContrast(ColorMath.ensureContrast(hexValue, base.bg, 3.0), base.surface, 3.0)
        return hex(a) to hex(ColorMath.readableOn(a))
    }
}

fun buildColors(kind: String, accentHex: String): DoonColors {
    val base = when (kind) {
        "dark" -> DARK
        "black" -> BLACK
        else -> LIGHT
    }
    val light = kind == "light"
    val accent = ColorMath.ensureContrast(ColorMath.ensureContrast(accentHex, base.bg, 3.0), base.surface, 3.0)
    val danger = "#C62F35"
    return DoonColors(
        kind = kind,
        bg = hex(base.bg),
        surface = hex(base.surface),
        ink = hex(base.ink),
        inkMuted = hex(base.inkMuted),
        hairline = base.hairline,
        accent = hex(accent),
        accentTint = hex(ColorMath.mix(base.bg, accent, if (light) 0.1 else 0.18)),
        accentWash = hex(ColorMath.mix(base.bg, accent, if (light) 0.05 else 0.08)),
        accentText = hex(ColorMath.ensureContrast(ColorMath.ensureContrast(accent, base.bg, 4.5), base.surface, 4.5)),
        onAccent = hex(ColorMath.readableOn(accent)),
        danger = hex(danger),
        dangerText = hex(ColorMath.ensureContrast(ColorMath.ensureContrast(danger, base.bg, 4.5), base.surface, 4.5)),
        onDanger = Color.White,
        bgHex = base.bg,
    )
}

val LocalColors = staticCompositionLocalOf { buildColors("light", "#1E9E9A") }

val colors: DoonColors @Composable get() = LocalColors.current

private val Display = FontFamily(
    Font(R.font.bricolage_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_bold, FontWeight.Bold),
)

private val TextFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private val trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
private val platform = PlatformTextStyle(includeFontPadding = false)

private fun style(family: FontFamily, weight: FontWeight, size: Int, line: Int) =
    TextStyle(fontFamily = family, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, lineHeightStyle = trim, platformStyle = platform)

object Type {
    val hero = style(Display, FontWeight.Bold, 88, 92)
    val numeral = style(Display, FontWeight.Bold, 56, 60)
    val title = style(Display, FontWeight.Bold, 28, 34)
    val h2 = style(Display, FontWeight.SemiBold, 20, 26)
    val body = style(TextFamily, FontWeight.Normal, 17, 23)
    val bodyStrong = style(TextFamily, FontWeight.SemiBold, 17, 23)
    val secondary = style(TextFamily, FontWeight.Normal, 15, 20)
    val secondaryStrong = style(TextFamily, FontWeight.Medium, 15, 20)
    val caption = style(TextFamily, FontWeight.Normal, 13, 17)
    val captionStrong = style(TextFamily, FontWeight.Medium, 13, 17)
    val micro = style(TextFamily, FontWeight.Medium, 11, 14)
}

const val MAX_FONT_SCALE = 1.3f

@Composable
fun DoonTheme(content: @Composable () -> Unit) {
    val system = isSystemInDarkTheme()
    val kind = when (Prefs.themeMode) {
        "system" -> if (system) "dark" else "light"
        else -> Prefs.themeMode
    }
    val accent = accentHex()
    val target = remember(kind, accent) { buildColors(kind, accent) }
    val animated by animateColorAsState(target.accent, tween(220), label = "accent")
    val palette = target.copy(accent = animated)
    val density = LocalDensity.current
    val clamped = remember(density) { Density(density.density, minOf(density.fontScale, MAX_FONT_SCALE)) }
    CompositionLocalProvider(
        LocalColors provides palette,
        LocalDensity provides clamped,
        LocalTextSelectionColors provides TextSelectionColors(target.accent, target.accent.copy(alpha = 0.3f)),
        content = content,
    )
}
