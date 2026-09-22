package com.jusdots.juscalc

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

// ponytail: BigDecimal + shunting-yard, no scripting engine dep; Double would drift on 0.1+0.2

object CalcEngine {
    private val MC = MathContext(12, RoundingMode.HALF_UP)

    sealed interface Eval { data class Ok(val value: BigDecimal) : Eval; data object Err : Eval }

    fun evaluate(expr: String): Eval {
        return try {
            val norm = expr.replace('×', '*').replace('÷', '/').replace('x', '*').replace('X', '*')
            if (norm.isBlank()) return Eval.Err
            Eval.Ok(evalTokens(tokenize(norm)))
        } catch (_: Exception) { Eval.Err }
    }

    private sealed interface Tok { data class Num(val v: BigDecimal) : Tok; data class Op(val c: Char) : Tok; data class Par(val c: Char) : Tok }

    private fun tokenize(s: String): List<Tok> {
        val out = mutableListOf<Tok>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isDigit() || c == '.' -> {
                    var j = i
                    while (j < s.length && (s[j].isDigit() || s[j] == '.')) j++
                    out += Tok.Num(BigDecimal(s.substring(i, j)))
                    i = j
                }
                c == '+' || c == '-' || c == '*' || c == '/' -> {
                    // unary minus
                    val prev = out.lastOrNull()
                    if (c == '-' && (prev == null || prev is Tok.Op || (prev is Tok.Par && prev.c == '('))) {
                        var j = i + 1
                        while (j < s.length && (s[j].isDigit() || s[j] == '.')) j++
                        if (j > i + 1) { out += Tok.Num(BigDecimal(s.substring(i, j))); i = j } else { out += Tok.Op(c); i++ }
                    } else { out += Tok.Op(c); i++ }
                }
                c == '(' || c == ')' -> { out += Tok.Par(c); i++ }
                c.isWhitespace() -> i++
                else -> i++
            }
        }
        return out
    }

    private fun prec(c: Char) = if (c == '+' || c == '-') 1 else 2

    private fun evalTokens(toks: List<Tok>): BigDecimal {
        val vals = ArrayDeque<BigDecimal>()
        val ops = ArrayDeque<Char>()
        fun apply() {
            val op = ops.removeLast()
            val b = vals.removeLast()
            val a = vals.removeLast()
            vals.addLast(when (op) {
                '+' -> a.add(b, MC)
                '-' -> a.subtract(b, MC)
                '*' -> a.multiply(b, MC)
                else -> {
                    if (b.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("div0")
                    a.divide(b, 10, RoundingMode.HALF_UP).stripTrailingZeros()
                }
            })
        }
        for (t in toks) when (t) {
            is Tok.Num -> vals.addLast(t.v)
            is Tok.Op -> { while (ops.isNotEmpty() && ops.last() != '(' && prec(ops.last()) >= prec(t.c)) apply(); ops.addLast(t.c) }
            is Tok.Par -> if (t.c == '(') ops.addLast('(') else { while (ops.isNotEmpty() && ops.last() != '(') apply(); if (ops.isEmpty()) throw IllegalArgumentException("paren"); ops.removeLast() }
        }
        while (ops.isNotEmpty()) { if (ops.last() == '(') throw IllegalArgumentException("paren"); apply() }
        if (vals.size != 1) throw IllegalArgumentException("expr")
        return vals.last().stripTrailingZeros()
    }

    fun formatResult(v: BigDecimal): String {
        val s = v.stripTrailingZeros().toPlainString()
        if (s.replace("-", "").replace(".", "").length > 15) {
            return v.round(MathContext(10)).stripTrailingZeros().toPlainString()
        }
        return s
    }

    // ---- words ----
    private val ONES = listOf("ZERO","ONE","TWO","THREE","FOUR","FIVE","SIX","SEVEN","EIGHT","NINE","TEN","ELEVEN","TWELVE","THIRTEEN","FOURTEEN","FIFTEEN","SIXTEEN","SEVENTEEN","EIGHTEEN","NINETEEN")
    private val TENS = listOf("", "", "TWENTY","THIRTY","FORTY","FIFTY","SIXTY","SEVENTY","EIGHTY","NINETY")
    private val SCALES = listOf("", "THOUSAND", "MILLION", "BILLION", "TRILLION")

    fun numberToWords(raw: String): String {
        val s = raw.trim().ifEmpty { return "" }
        return try {
            if (s.contains('.')) {
                val (ip, fp) = s.split('.', limit = 2)
                val head = if (ip.isEmpty() || ip == "-") "ZERO" else longToWords(ip.toLong())
                val neg = if (s.startsWith("-")) "" else "" // longToWords handles sign
                val tail = fp.take(6).map { if (it.isDigit()) ONES[it - '0'] else "" }.filter { it.isNotEmpty() }
                if (tail.isEmpty()) head else "$head POINT ${tail.joinToString(" ")}"
            } else longToWords(s.toLong())
        } catch (_: Exception) {
            // fallback for huge/decimal via BigDecimal: spell integer part only
            try {
                val bd = BigDecimal(s)
                val ip = bd.toLong()
                val head = longToWords(ip)
                val frac = s.substringAfter('.', "").take(6)
                if (frac.isEmpty()) head else "$head POINT ${frac.map { ONES[it - '0'] }.joinToString(" ")}"
            } catch (_: Exception) { s.uppercase() }
        }
    }

    fun longToWords(n: Long): String {
        if (n == 0L) return "ZERO"
        if (n < 0) return "MINUS ${longToWords(-n)}"
        val parts = mutableListOf<String>()
        var num = n; var si = 0
        while (num > 0) {
            val chunk = (num % 1000).toInt()
            if (chunk != 0) {
                val w = chunkToWords(chunk)
                parts.add(0, if (SCALES[si].isEmpty()) w else "$w ${SCALES[si]}")
            }
            num /= 1000; si++
            if (si >= SCALES.size) { parts.add(0, chunkToWords((num % 1000).toInt())); break }
        }
        return parts.joinToString(" ")
    }

    private fun chunkToWords(n: Int): String {
        val out = mutableListOf<String>()
        val h = n / 100; val r = n % 100
        if (h > 0) out += "${ONES[h]} HUNDRED"
        if (r > 0) {
            if (r < 20) out += ONES[r]
            else { out += if (r % 10 == 0) TENS[r / 10] else "${TENS[r / 10]} ${ONES[r % 10]}" }
        }
        return out.joinToString(" ")
    }
}
