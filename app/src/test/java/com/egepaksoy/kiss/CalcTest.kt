package com.egepaksoy.kiss

import com.egepaksoy.kiss.ui.calc.Calc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcTest {
    private fun eval(s: String) = Calc.evaluate(s)?.let { Calc.format(it) }

    @Test fun precedence() = assertEquals("14", eval("2+3×4"))
    @Test fun decimals() = assertEquals("0,3", eval("0.1+0.2"))
    @Test fun division() = assertEquals("3,33333333333", eval("10÷3"))
    @Test fun percent() = assertEquals("50", eval("200×25%"))
    @Test fun negative() = assertEquals("−2", eval("−5+3"))
    @Test fun grouping() = assertEquals("1.234.567", eval("1234567×1"))
    @Test fun divideByZero() = assertNull(eval("5÷0"))
    @Test fun trailingOperator() = assertNull(eval("5+"))
}
