package com.egepaksoy.kiss.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.data.Alarm
import com.egepaksoy.kiss.data.Apps
import com.egepaksoy.kiss.data.KissSettings
import com.egepaksoy.kiss.data.Sms
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
    val apps by produceState(emptyList(), s.allowedApps, tick) {
        value = withContext(Dispatchers.IO) { Apps.entries(ctx, s.allowedApps) }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(start = 28.dp, end = 20.dp, top = 28.dp, bottom = 16.dp)) {
        Text(
            now.format(DateTimeFormatter.ofPattern("HH:mm")),
            fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 80.sp, lineHeight = 80.sp,
            letterSpacing = (-0.045).em,
        )
        Text(
            now.format(DateTimeFormatter.ofPattern("d MMMM EEEE", Locale.forLanguageTag("tr"))),
            color = c.muted, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp, start = 2.dp),
        )

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 36.dp)) {
            HomeItem("Ara") { nav.push(Screen.Dialer) }
            HomeItem("Mesajlar", badge = unread) { nav.push(Screen.Messages) }
            if (s.camera) HomeItem("Kamera") { nav.push(Screen.Camera) }
            if (s.gallery) HomeItem("Galeri") { nav.push(Screen.Gallery) }
            if (s.notes) HomeItem("Notlar") {
                val pkg = s.notesPackage
                if (pkg != null) Apps.launch(ctx, pkg) else nav.push(Screen.AppPicker(notes = true))
            }
            if (s.calculator) HomeItem("Hesap makinesi") { nav.push(Screen.Calculator) }
            if (s.alarm) HomeItem("Alarm", sub = nextAlarm) { Alarm.open(ctx) }

            if (apps.isNotEmpty()) {
                Row(Modifier.padding(top = 22.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("UYGULAMALAR", fontSize = 11.sp, letterSpacing = 1.2.sp, color = c.muted, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f).height(1.dp).background(c.line))
                }
                apps.forEach { a -> HomeItem(a.label, size = 22.sp) { Apps.launch(ctx, a.pkg) } }
            }
            Spacer(Modifier.height(12.dp))
        }

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
private fun HomeItem(label: String, badge: Int = 0, sub: String? = null, size: TextUnit = 32.sp, onClick: () -> Unit) {
    val c = Kiss.colors
    Row(
        Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = size, lineHeight = size, letterSpacing = (-0.02).em)
        if (badge > 0) {
            Box(Modifier.clip(CircleShape).background(c.accent).padding(horizontal = 9.dp, vertical = 2.dp)) {
                Text(badge.toString(), color = c.onAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (sub != null) Text(sub, color = c.muted, fontSize = 15.sp)
    }
}
