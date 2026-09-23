package com.jusdots.juscalc

import org.junit.Assert.*
import org.junit.Test

class CalcEngineTest {
    private fun ok(expr: String): String {
        val r = CalcEngine.evaluate(expr)
        assertTrue("expected Ok for $expr", r is CalcEngine.Eval.Ok)
        return (r as CalcEngine.Eval.Ok).value.toPlainString()
    }

    @Test fun arithmetic() {
        assertEquals("7", ok("1+2*3"))
        assertEquals("9", ok("(1+2)*3"))
        assertEquals("14", ok("2*(3+4)"))
        assertEquals("2.5", ok("10/4"))
        assertEquals("-2", ok("-5+3"))
        assertEquals("0.3", ok("0.1+0.2")) // exact decimals, no Double drift
        assertEquals("6", ok("2×3"))
        assertEquals("4", ok("8÷2"))
    }

    @Test fun errors() {
        assertTrue(CalcEngine.evaluate("") is CalcEngine.Eval.Err)
        assertTrue(CalcEngine.evaluate("5/0") is CalcEngine.Eval.Err)
        assertTrue(CalcEngine.evaluate("5+") is CalcEngine.Eval.Err)
        assertTrue(CalcEngine.evaluate("(1+2") is CalcEngine.Eval.Err)
    }

    @Test fun compact() {
        assertEquals("55 THOUSAND", CalcEngine.numberToCompact("55000"))
        assertEquals("69 THOUSAND", CalcEngine.numberToCompact("69000"))
        assertEquals("1.2 MILLION", CalcEngine.numberToCompact("1200000"))
        assertEquals("7.5 MILLION", CalcEngine.numberToCompact("7500000"))
        assertEquals("420 MILLION", CalcEngine.numberToCompact("420000000"))
        assertEquals("1 BILLION", CalcEngine.numberToCompact("1000000000"))
        assertEquals("1 THOUSAND", CalcEngine.numberToCompact("1000"))
        assertEquals("1.5 THOUSAND", CalcEngine.numberToCompact("1500"))
        assertEquals("42", CalcEngine.numberToCompact("42"))
        assertEquals("0.3", CalcEngine.numberToCompact("0.3"))
        assertEquals("0", CalcEngine.numberToCompact("0"))
        assertEquals("-5 THOUSAND", CalcEngine.numberToCompact("-5000"))
        assertEquals("1 TRILLION", CalcEngine.numberToCompact("999999999999.9"))
    }
}
