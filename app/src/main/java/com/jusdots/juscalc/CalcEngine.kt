package com.jusdots.juscalc

import com.ezylang.evalex.Expression
import com.ezylang.evalex.config.ExpressionConfiguration
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

// Parsing + evaluation is EvalEx (com.ezylang:EvalEx, Apache-2.0): BigDecimal
// math, no scripting engine, no hand-rolled parser. Anything it rejects
// (trailing operator, div-by-zero, bad parens) surfaces as Err.
object CalcEngine {
    private val CONFIG = ExpressionConfiguration.builder()
        .mathContext(MathContext(12, RoundingMode.HALF_UP))
        .build()

    sealed interface Eval { data class Ok(val value: BigDecimal) : Eval; data object Err : Eval }

    fun evaluate(expr: String): Eval {
        return try {
            if (expr.isBlank()) return Eval.Err
            val norm = expr.replace('×', '*').replace('÷', '/').replace('x', '*').replace('X', '*')
            val result = Expression(norm, CONFIG).evaluate().numberValue ?: return Eval.Err
            Eval.Ok(result.stripTrailingZeros())
        } catch (_: Exception) { Eval.Err }
    }

    fun formatResult(v: BigDecimal): String {
        val s = v.stripTrailingZeros().toPlainString()
        if (s.replace("-", "").replace(".", "").length > 15) {
            return v.round(MathContext(10)).stripTrailingZeros().toPlainString()
        }
        return s
    }

    // ---- compact magnitudes: digits + scale word, e.g. 55000 -> "55 THOUSAND",
    // 1200000 -> "1.2 MILLION". Below a thousand, plain digits ("42", "0.3"). ----
    private val SCALES = listOf(
        BigDecimal("1000000000000000000") to "QUINTILLION",
        BigDecimal("1000000000000000") to "QUADRILLION",
        BigDecimal("1000000000000") to "TRILLION",
        BigDecimal("1000000000") to "BILLION",
        BigDecimal("1000000") to "MILLION",
        BigDecimal("1000") to "THOUSAND",
    )

    fun numberToCompact(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return ""
        val v = try { BigDecimal(s) } catch (_: Exception) { return s.uppercase() }
        if (v.compareTo(BigDecimal.ZERO) == 0) return "0"
        val sign = if (v.signum() < 0) "-" else ""
        val a = v.abs()
        if (a < BigDecimal(1000)) return sign + a.stripTrailingZeros().toPlainString()
        var idx = SCALES.indexOfFirst { a >= it.first }.coerceAtLeast(SCALES.lastIndex)
        var q = a.divide(SCALES[idx].first, 3, RoundingMode.HALF_UP).stripTrailingZeros()
        // Rounding can push the quotient to 1000 (999999.9 -> "1000 THOUSAND"),
        // so step up a scale when there is room above.
        while (q.compareTo(BigDecimal(1000)) >= 0 && idx > 0) {
            idx--
            q = a.divide(SCALES[idx].first, 3, RoundingMode.HALF_UP).stripTrailingZeros()
        }
        return sign + q.toPlainString() + " " + SCALES[idx].second
    }
}
