package com.egepaksoy.kiss.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.data.KissSettings
import com.egepaksoy.kiss.data.Pin
import com.egepaksoy.kiss.ui.Navigator
import com.egepaksoy.kiss.ui.Screen
import com.egepaksoy.kiss.ui.common.TopBar
import com.egepaksoy.kiss.ui.common.Ticker
import com.egepaksoy.kiss.ui.theme.Display
import com.egepaksoy.kiss.ui.theme.Kiss

/** Ayarlara giriş: önce bekleme, sonra PIN. Ekrandan çıkınca sayaç baştan başlar. */
@Composable
fun GateScreen(s: KissSettings, nav: Navigator) {
    var left by remember { mutableIntStateOf(s.waitSeconds) }
    var error by remember { mutableStateOf<String?>(null) }
    Ticker(s.waitSeconds) { left = it }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar("Ayarlar", { nav.pop() })
        if (left > 0) {
            WaitRing(left, s.waitSeconds, "Biraz bekle", "Bu değişikliği gerçekten istiyor musun? Süre dolunca PIN sorulacak.")
        } else {
            PinEntry("PIN gir", "Ayarları açmak için 4 haneli PIN", error) { p ->
                if (Pin.verify(p, s.pin)) { nav.replace(Screen.Settings); true }
                else { error = "PIN yanlış"; false }
            }
        }
    }
}

@Composable
fun WaitRing(left: Int, total: Int, title: String, body: String) {
    val c = Kiss.colors
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = 10.dp.toPx()
                val inset = w / 2
                val arc = Size(size.width - w, size.height - w)
                drawArc(c.soft, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(w))
                val sweep = if (total == 0) 360f else 360f * (total - left) / total
                drawArc(c.accent, -90f, sweep, false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
            }
            val txt = if (left >= 60) "%d:%02d".format(left / 60, left % 60) else left.toString()
            Text(txt, fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 52.sp)
        }
        Spacer(Modifier.height(28.dp))
        Text(title, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
        Text(body, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    }
}
