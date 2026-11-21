package com.madebykunal.doon.lib

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

object ColorMath {
    private fun rgb(hex: String): IntArray {
        val n = hex.removePrefix("#").toLong(16).toInt()
        return intArrayOf((n shr 16) and 255, (n shr 8) and 255, n and 255)
    }

    private fun hex(r: Double, g: Double, b: Double): String {
        fun part(v: Double) = min(255.0, max(0.0, v)).roundToInt().toString(16).padStart(2, '0')
        return "#${part(r)}${part(g)}${part(b)}".uppercase()
    }

    fun toArgb(hex: String): Int = (0xFF shl 24) or hex.removePrefix("#").toLong(16).toInt()

    fun mix(a: String, b: String, amount: Double): String {
        val x = rgb(a)
        val y = rgb(b)
        return hex(
            x[0] + (y[0] - x[0]) * amount,
            x[1] + (y[1] - x[1]) * amount,
            x[2] + (y[2] - x[2]) * amount,
        )
    }

    private fun channel(v: Int): Double {
        val s = v / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(hex: String): Double {
        val c = rgb(hex)
        return 0.2126 * channel(c[0]) + 0.7152 * channel(c[1]) + 0.0722 * channel(c[2])
    }

    fun contrast(a: String, b: String): Double {
        val x = luminance(a)
        val y = luminance(b)
        return (max(x, y) + 0.05) / (min(x, y) + 0.05)
    }

    fun ensureContrast(color: String, bg: String, ratio: Double): String {
        if (contrast(color, bg) >= ratio) return color
        val target = if (luminance(bg) < 0.5) "#FFFFFF" else "#000000"
        var step = 0.04
        while (step <= 1) {
            val candidate = mix(color, target, step)
            if (contrast(candidate, bg) >= ratio) return candidate
            step += 0.04
        }
        return target
    }

    fun readableOn(bg: String): String =
        if (contrast("#FFFFFF", bg) >= contrast("#000000", bg)) "#FFFFFF" else "#000000"

    fun hsvToHex(h: Double, s: Double, v: Double): String {
        val chroma = v * s
        val x = chroma * (1 - abs(((h / 60) % 2) - 1))
        val m = v - chroma
        val sectors = arrayOf(
            doubleArrayOf(chroma, x, 0.0),
            doubleArrayOf(x, chroma, 0.0),
            doubleArrayOf(0.0, chroma, x),
            doubleArrayOf(0.0, x, chroma),
            doubleArrayOf(x, 0.0, chroma),
            doubleArrayOf(chroma, 0.0, x),
        )
        val c = sectors[(floor(h / 60).toInt() % 6 + 6) % 6]
        return hex((c[0] + m) * 255, (c[1] + m) * 255, (c[2] + m) * 255)
    }

    fun hexToHsv(hex: String): DoubleArray {
        val c = rgb(hex)
        val r = c[0].toDouble()
        val g = c[1].toDouble()
        val b = c[2].toDouble()
        val mx = max(r, max(g, b))
        val delta = mx - min(r, min(g, b))
        var h = 0.0
        if (delta > 0) {
            h = when (mx) {
                r -> ((g - b) / delta + 6) % 6
                g -> (b - r) / delta + 2
                else -> (r - g) / delta + 4
            }
        }
        return doubleArrayOf(h * 60, if (mx == 0.0) 0.0 else delta / mx, mx / 255)
    }
}
