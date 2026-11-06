package com.egepaksoy.kiss.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.ui.common.PrimaryButton
import com.egepaksoy.kiss.ui.theme.Display
import com.egepaksoy.kiss.ui.theme.Kiss

/** KISS kapalıyken uygulama listesinden açılınca görünen ekran. */
@Composable
fun PausedScreen() {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(28.dp)) {
        Spacer(Modifier.weight(1f))
        Text("KISS kapalı", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 48.sp, letterSpacing = (-0.03).em, lineHeight = 52.sp)
        Spacer(Modifier.height(14.dp))
        Text(
            "Telefon şu an normal ana ekranla çalışıyor. KISS'i açınca ana ekran tuşu yeniden KISS'e gelir.",
            color = Kiss.colors.muted, fontSize = 17.sp, lineHeight = 25.sp,
        )
        Spacer(Modifier.weight(1.4f))
        PrimaryButton("KISS'i aç", {
            KissApp.instance.settings.update { it.copy(enabled = true) }
            KissApp.instance.nav.home()
        }, Modifier.fillMaxWidth())
    }
}
