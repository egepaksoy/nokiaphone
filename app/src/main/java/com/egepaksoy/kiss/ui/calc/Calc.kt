package com.egepaksoy.kiss.ui.calc

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Dört işlem ve yüzde. İfade içinde: rakamlar, '.', + − × ÷ ve %. */
object Calc {
    private val MC = MathContext(16, RoundingMode.HALF_EVEN)
    const val OPS = "+−×÷"

    fun evaluate(expr: String): BigDecimal? {
        val tokens = tokenize(expr) ?: return null
        if (tokens.isEmpty()) return null
        val out = ArrayDeque<BigDecimal>()
        val ops = ArrayDeque<Char>()
        fun prec(o: Char) = if (o == '×' || o == '÷') 2 else 1
        fun reduce(): Boolean {
            if (out.size < 2) return false
            val b = out.removeLast(); val a = out.removeLast()
            val r = when (ops.removeLast()) {
                '+' -> a.add(b, MC)
                '−' -> a.subtract(b, MC)
                '×' -> a.multiply(b, MC)
                '÷' -> if (b.signum() == 0) return false else a.divide(b, MC)
                else -> return false
            }
            out.addLast(r)
            return true
        }
        var expectNumber = true
        for (t in tokens) {
            if (t is BigDecimal) { out.addLast(t); expectNumber = false }
            else {
                val o = t as Char
                if (expectNumber) return null
                while (ops.isNotEmpty() && prec(ops.last()) >= prec(o)) if (!reduce()) return null
                ops.addLast(o); expectNumber = true
            }
        }
        if (expectNumber) return null
        while (ops.isNotEmpty()) if (!reduce()) return null
        return out.singleOrNull()
    }

    private fun tokenize(s: String): List<Any>? {
        val list = ArrayList<Any>()
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            when {
                ch.isDigit() || ch == '.' -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    var n = s.substring(start, i).toBigDecimalOrNull() ?: return null
                    while (i < s.length && s[i] == '%') { n = n.divide(BigDecimal(100), MC); i++ }
                    // Önceki işaret "−" ve ondan önce sayı yoksa negatif sayı
                    list += n
                    continue
                }
                ch in OPS -> {
                    if (ch == '−' && (list.isEmpty() || list.last() is Char)) {
                        // tekli eksi: bir sonraki sayıya uygula
                        i++
                        val start = i
                        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                        if (start == i) return null
                        var n = s.substring(start, i).toBigDecimalOrNull()?.negate() ?: return null
                        while (i < s.length && s[i] == '%') { n = n.divide(BigDecimal(100), MC); i++ }
                        list += n
                        continue
                    }
                    list += ch
                }
                ch == ' ' -> {}
                else -> return null
            }
            i++
        }
        return list
    }

    fun format(v: BigDecimal): String {
        val r = v.round(MathContext(12, RoundingMode.HALF_EVEN)).stripTrailingZeros()
        val plain = if (r.abs() >= BigDecimal("1e12") || (r.signum() != 0 && r.abs() < BigDecimal("1e-9")))
            r.toString() else r.toPlainString()
        return display(plain.replace("E+", "e").replace("E", "e").replace('-', '−'))
    }

    /** İç gösterim ('.') → Türkçe gösterim (','). Binlik ayırıcı eklenir. */
    fun display(expr: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < expr.length) {
            if (expr[i].isDigit()) {
                val start = i
                while (i < expr.length && expr[i].isDigit()) i++
                val intPart = expr.substring(start, i)
                sb.append(group(intPart))
                if (i < expr.length && expr[i] == '.') {
                    sb.append(',')
                    i++
                    val fs = i
                    while (i < expr.length && expr[i].isDigit()) i++
                    sb.append(expr, fs, i)
                }
            } else {
                sb.append(if (expr[i] == '.') ',' else expr[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun group(d: String): String {
        if (d.length <= 3) return d
        val sb = StringBuilder()
        d.reversed().forEachIndexed { idx, ch -> if (idx > 0 && idx % 3 == 0) sb.append('.'); sb.append(ch) }
        return sb.reverse().toString()
    }
}
