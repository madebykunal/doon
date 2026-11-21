package com.madebykunal.doon.lib

object FractionalIndex {
    private const val DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private val ZERO = DIGITS[0]
    private val SMALLEST_INTEGER = "A" + ZERO.toString().repeat(26)

    private fun midpoint(a: String, b: String?): String {
        if (b != null && a >= b) throw IllegalArgumentException("order")
        if (a.lastOrNull() == ZERO || (b != null && b.lastOrNull() == ZERO)) throw IllegalArgumentException("trailing zero")
        if (b != null) {
            var n = 0
            while (n < b.length && (a.getOrNull(n) ?: ZERO) == b[n]) n++
            if (n > 0) return b.substring(0, n) + midpoint(a.substring(minOf(n, a.length)), b.substring(n))
        }
        val digitA = if (a.isNotEmpty()) DIGITS.indexOf(a[0]) else 0
        val digitB = if (b != null) DIGITS.indexOf(b[0]) else DIGITS.length
        return if (digitB - digitA > 1) {
            DIGITS[Math.round(0.5 * (digitA + digitB)).toInt()].toString()
        } else if (b != null && b.length > 1) {
            b.substring(0, 1)
        } else {
            DIGITS[digitA] + midpoint(if (a.isNotEmpty()) a.substring(1) else "", null)
        }
    }

    private fun integerLength(head: Char): Int = when (head) {
        in 'a'..'z' -> head - 'a' + 2
        in 'A'..'Z' -> 'Z' - head + 2
        else -> throw IllegalArgumentException("head")
    }

    private fun integerPart(key: String): String {
        val length = integerLength(key[0])
        if (length > key.length) throw IllegalArgumentException("key")
        return key.substring(0, length)
    }

    private fun validate(key: String) {
        if (key == SMALLEST_INTEGER) throw IllegalArgumentException("key")
        val i = integerPart(key)
        if (key.substring(i.length).lastOrNull() == ZERO) throw IllegalArgumentException("key")
    }

    private fun increment(x: String): String? {
        val head = x[0]
        val digs = x.substring(1).toCharArray().toMutableList()
        var carry = true
        var i = digs.size - 1
        while (carry && i >= 0) {
            val d = DIGITS.indexOf(digs[i]) + 1
            if (d == DIGITS.length) digs[i] = ZERO else {
                digs[i] = DIGITS[d]
                carry = false
            }
            i--
        }
        if (!carry) return head + digs.joinToString("")
        if (head == 'Z') return "a$ZERO"
        if (head == 'z') return null
        val h = head + 1
        if (h > 'a') digs.add(ZERO) else digs.removeAt(digs.size - 1)
        return h + digs.joinToString("")
    }

    private fun decrement(x: String): String? {
        val head = x[0]
        val digs = x.substring(1).toCharArray().toMutableList()
        var borrow = true
        var i = digs.size - 1
        while (borrow && i >= 0) {
            val d = DIGITS.indexOf(digs[i]) - 1
            if (d == -1) digs[i] = DIGITS.last() else {
                digs[i] = DIGITS[d]
                borrow = false
            }
            i--
        }
        if (!borrow) return head + digs.joinToString("")
        if (head == 'a') return "Z" + DIGITS.last()
        if (head == 'A') return null
        val h = head - 1
        if (h < 'Z') digs.add(DIGITS.last()) else digs.removeAt(digs.size - 1)
        return h + digs.joinToString("")
    }

    fun between(a: String?, b: String?): String {
        if (a != null) validate(a)
        if (b != null) validate(b)
        if (a != null && b != null && a >= b) throw IllegalArgumentException("order")
        if (a == null) {
            if (b == null) return "a$ZERO"
            val ib = integerPart(b)
            val fb = b.substring(ib.length)
            if (ib == SMALLEST_INTEGER) return ib + midpoint("", fb)
            if (ib < b) return ib
            return decrement(ib) ?: throw IllegalArgumentException("range")
        }
        if (b == null) {
            val ia = integerPart(a)
            val fa = a.substring(ia.length)
            return increment(ia) ?: (ia + midpoint(fa, null))
        }
        val ia = integerPart(a)
        val fa = a.substring(ia.length)
        val ib = integerPart(b)
        val fb = b.substring(ib.length)
        if (ia == ib) return ia + midpoint(fa, fb)
        val i = increment(ia) ?: throw IllegalArgumentException("range")
        if (i < b) return i
        return ia + midpoint(fa, null)
    }

    fun nBetween(a: String?, b: String?, n: Int): List<String> {
        if (n == 0) return emptyList()
        if (n == 1) return listOf(between(a, b))
        if (b == null) {
            var c = between(a, null)
            val result = mutableListOf(c)
            repeat(n - 1) {
                c = between(c, null)
                result.add(c)
            }
            return result
        }
        if (a == null) {
            var c = between(null, b)
            val result = mutableListOf(c)
            repeat(n - 1) {
                c = between(null, c)
                result.add(c)
            }
            return result.reversed()
        }
        val mid = n / 2
        val c = between(a, b)
        return nBetween(a, c, mid) + c + nBetween(c, b, n - mid - 1)
    }
}
