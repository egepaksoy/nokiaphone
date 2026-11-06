package com.egepaksoy.kiss.ui.calc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.ui.Navigator
import com.egepaksoy.kiss.ui.common.KIcons
import com.egepaksoy.kiss.ui.common.TopBar
import com.egepaksoy.kiss.ui.theme.Display
import com.egepaksoy.kiss.ui.theme.Kiss

@Composable
fun CalculatorScreen(nav: Navigator) {
    val c = Kiss.colors
    var expr by rememberSaveable { mutableStateOf("") }
    var justEvaluated by rememberSaveable { mutableStateOf(false) }
    val preview = remember(expr) { Calc.evaluate(expr)?.let { Calc.format(it) } }

    fun press(k: String) {
        when (k) {
            "AC" -> { expr = ""; justEvaluated = false }
            "⌫" -> { expr = expr.dropLast(1); justEvaluated = false }
            "=" -> {
                val v = Calc.evaluate(expr)
                if (v != null) { expr = v.round(java.math.MathContext(14)).stripTrailingZeros().toPlainString().replace('-', '−'); justEvaluated = true }
            }
            "%" -> if (expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == '%')) expr += "%"
            "," -> {
                if (justEvaluated) { expr = "0."; justEvaluated = false; return }
                val lastNum = expr.takeLastWhile { it.isDigit() || it == '.' }
                if ('.' !in lastNum) expr += if (lastNum.isEmpty()) "0." else "."
            }
            in listOf("+", "−", "×", "÷") -> {
                justEvaluated = false
                expr = when {
                    expr.isEmpty() -> if (k == "−") "−" else expr
                    expr.last() in Calc.OPS -> expr.dropLast(1) + k
                    expr.last() == '.' -> expr.dropLast(1) + k
                    else -> expr + k
                }
            }
            else -> {
                if (justEvaluated) { expr = ""; justEvaluated = false }
                if (expr.length < 40) expr += k
            }
        }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar("Hesap makinesi", { nav.pop() })
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
            Text(
                if (expr.isEmpty()) "0" else Calc.display(expr),
                fontFamily = Display, fontWeight = FontWeight.Medium,
                fontSize = when { expr.length > 18 -> 32.sp; expr.length > 11 -> 42.sp; else -> 58.sp },
                textAlign = TextAlign.End, lineHeight = 64.sp,
            )
            Text(
                if (!justEvaluated && preview != null && expr.any { it in Calc.OPS || it == '%' }) preview else " ",
                color = c.muted, fontSize = 24.sp, fontFamily = Display, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )
        }
        val rows = listOf(
            listOf("AC", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("00", "0", ",", "="),
        )
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { k ->
                        val op = k in listOf("÷", "×", "−", "+")
                        val eq = k == "="
                        val fn = k in listOf("AC", "⌫", "%")
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                                .background(when { eq -> c.accent; op -> c.fg; fn -> c.line; else -> c.soft })
                                .clickable { press(k) },
                            contentAlignment = Alignment.Center,
                        ) {
                            val color: Color = when { eq -> c.onAccent; op -> c.bg; else -> c.fg }
                            if (k == "⌫") Icon(KIcons.Backspace, "Sil", tint = color, modifier = Modifier.size(26.dp))
                            else Text(k, fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = if (k == "AC") 22.sp else 30.sp, color = color)
                        }
                    }
                }
            }
            Spacer(Modifier.size(4.dp))
        }
    }
}
