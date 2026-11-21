package com.madebykunal.doon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class Ic(vararg val d: String) {
    ArrowRight("M5 12h14", "m12 5 7 7-7 7"),
    Bell("M10.268 21a2 2 0 0 0 3.464 0", "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326"),
    Check("M20 6 9 17l-5-5"),
    ChevronDown("m6 9 6 6 6-6"),
    ChevronUp("m18 15-6-6-6 6"),
    ChevronLeft("m15 18-6-6 6-6"),
    ChevronRight("m9 18 6-6-6-6"),
    FolderPlus("M12 10v6", "M9 13h6", "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"),
    Heading("M4 12h8", "M4 18V6", "M12 18V6", "m17 12 3-2v8"),
    Pencil("M4 20v-4L15 5l4 4L8 20z", "M12 8l4 4", "M13 20h7"),
    Plus("M5 12h14", "M12 5v14"),
    Minus("M5 12h14"),
    Menu("M4 7h16", "M4 12h16", "M4 17h16"),
    Reorder("m3 8 4-4 4 4", "M7 4v16", "m21 16-4 4-4-4", "M17 20V4"),
    Grip("M5 9h14", "M5 15h14"),
    Calendar("M8 2v4", "M16 2v4", "M3 10h18", "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"),
    Repeat("m17 2 4 4-4 4", "M3 11v-1a4 4 0 0 1 4-4h14", "m7 22-4-4 4-4", "M21 13v1a4 4 0 0 1-4 4H3"),
    Search("M3 11a8 8 0 1 0 16 0a8 8 0 1 0-16 0", "m21 21-4.34-4.34"),
    Settings(
        "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z",
        "M9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0",
    ),
    Trash("M3 6h18", "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6", "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"),
    X("M18 6 6 18", "m6 6 12 12"),
    TextLines("M21 6H3", "M15 12H3", "M17 18H3");

    val paths: List<Path> by lazy { d.map { parsePath(it) } }
}

fun parsePath(d: String): Path = PathParser().parsePathString(d).toPath()

@Composable
fun Icon(ic: Ic, color: Color, size: Dp = 20.dp, stroke: Float = 2f, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, Offset.Zero) {
            val sharp = ic == Ic.Pencil
            val style = Stroke(width = stroke, cap = if (sharp) StrokeCap.Butt else StrokeCap.Round, join = if (sharp) StrokeJoin.Miter else StrokeJoin.Round)
            ic.paths.forEach { drawPath(it, color, style = style) }
        }
    }
}
