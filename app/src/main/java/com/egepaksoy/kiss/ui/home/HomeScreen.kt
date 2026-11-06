package com.egepaksoy.kiss.ui.home

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.data.Alarm
import com.egepaksoy.kiss.data.KissSettings
import com.egepaksoy.kiss.data.Sms
import com.egepaksoy.kiss.lock.ScreenLock
import com.egepaksoy.kiss.ui.Navigator
import com.egepaksoy.kiss.ui.Screen
import com.egepaksoy.kiss.ui.common.KIcons
import com.egepaksoy.kiss.ui.common.RoundBtn
import com.egepaksoy.kiss.ui.theme.Display
import com.egepaksoy.kiss.ui.theme.Kiss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ana ekran: saat, Telefon, Uygulamalar. Başka hiçbir şey. */
@Composable
fun HomeScreen(s: KissSettings, nav: Navigator) {
    val ctx = LocalContext.current
    val app = KissApp.instance
    val c = Kiss.colors
    val tick by app.resumeTick.collectAsState()
    val torchOn by app.torch.on.collectAsState()

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay((60 - now.second) * 1000L + 50)
        }
    }
    val unread by produceState(0, tick) {
        value = withContext(Dispatchers.IO) { Sms.unreadCount(ctx) }
        Sms.changes(ctx).collect { value = withContext(Dispatchers.IO) { Sms.unreadCount(ctx) } }
    }
    val nextAlarm = remember(tick, now.minute) { if (s.alarm) Alarm.next(ctx) else null }

    Column(
        Modifier.fillMaxSize()
            // Boş bir yere iki kez dokun: ekranı kilitle
            .pointerInput(s.doubleTapLock) {
                if (s.doubleTapLock) detectTapGestures(onDoubleTap = {
                    (ctx as? Activity)?.let { ScreenLock.lock(it) }
                })
            }
            .systemBarsPadding().padding(start = 28.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
    ) {
        // Saate dokun: Saat uygulaması (alarmlar orada ayarlanır)
        Column(
            Modifier.clip(RoundedCornerShape(16.dp))
                .then(if (s.alarm) Modifier.clickable { Alarm.open(ctx) } else Modifier)
                .padding(end = 12.dp, bottom = 4.dp),
        ) {
            Text(
                now.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 80.sp, lineHeight = 80.sp,
                letterSpacing = (-0.045).em,
            )
            Row(Modifier.padding(top = 6.dp, start = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    now.format(DateTimeFormatter.ofPattern("d MMMM EEEE", Locale.forLanguageTag("tr"))),
                    color = c.muted, fontSize = 16.sp,
                )
                if (nextAlarm != null) {
                    Text("  ·  ", color = c.muted, fontSize = 16.sp)
                    Icon(KIcons.Alarm, null, tint = c.muted, modifier = Modifier.size(15.dp))
                    Text(" $nextAlarm", color = c.muted, fontSize = 16.sp)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        BigItem("Telefon", badge = unread) { nav.push(Screen.Phone()) }
        BigItem("Uygulamalar") { nav.push(Screen.AppsList) }
        Spacer(Modifier.weight(0.35f))

        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (s.torch && app.torch.available) {
                RoundBtn(
                    KIcons.Torch, if (torchOn) "Feneri kapat" else "Feneri aç", { app.torch.toggle() },
                    bg = if (torchOn) c.fg else c.soft, fg = if (torchOn) c.bg else c.fg, size = 52.dp,
                )
            }
            Spacer(Modifier.weight(1f))
            RoundBtn(KIcons.Gear, "Ayarlar", { nav.push(Screen.Gate) }, size = 52.dp)
        }
    }
}

@Composable
fun BigItem(label: String, badge: Int = 0, size: TextUnit = 40.sp, onClick: () -> Unit) {
    val c = Kiss.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = size, lineHeight = size, letterSpacing = (-0.025).em)
        if (badge > 0) {
            Box(Modifier.clip(CircleShape).background(c.accent).padding(horizontal = 10.dp, vertical = 2.dp)) {
                Text(badge.toString(), color = c.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
