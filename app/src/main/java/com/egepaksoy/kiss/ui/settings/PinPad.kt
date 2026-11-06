package com.egepaksoy.kiss.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.ui.common.IconBtn
import com.egepaksoy.kiss.ui.common.KIcons
import com.egepaksoy.kiss.ui.common.Keypad
import com.egepaksoy.kiss.ui.theme.Display
import com.egepaksoy.kiss.ui.theme.Kiss

const val PIN_LENGTH = 4

/**
 * 4 haneli PIN girişi. [onComplete] true dönerse giriş kabul edilir, false dönerse sallanıp temizlenir.
 */
@Composable
fun PinEntry(title: String, subtitle: String, error: String? = null, onComplete: (String) -> Boolean) {
    val c = Kiss.colors
    var pin by remember { mutableStateOf("") }
    var shakeKey by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey > 0) {
            for (x in listOf(-18f, 16f, -12f, 8f, 0f)) shake.animateTo(x, spring(stiffness = 4000f))
        }
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(title, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, textAlign = TextAlign.Center)
        Text(subtitle, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 26.dp).offset(x = shake.value.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(PIN_LENGTH) { i ->
                Box(
                    Modifier.size(15.dp).clip(CircleShape)
                        .then(if (i < pin.length) Modifier.background(c.fg) else Modifier.border(2.dp, c.fg, CircleShape)),
                )
            }
        }
        Text(error ?: " ", color = c.end, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.weight(1f))
        Keypad(
            onKey = { k ->
                if (!k.isDigit() || pin.length >= PIN_LENGTH) return@Keypad
                pin += k
                if (pin.length == PIN_LENGTH) {
                    val ok = onComplete(pin)
                    if (!ok) shakeKey++
                    pin = ""
                }
            },
            letters = false, keySize = 70.dp, gapV = 12.dp,
        )
        Box(Modifier.height(64.dp), contentAlignment = Alignment.Center) {
            if (pin.isNotEmpty()) IconBtn(KIcons.Backspace, "Sil", { pin = pin.dropLast(1) }, tint = c.muted)
        }
    }
}
