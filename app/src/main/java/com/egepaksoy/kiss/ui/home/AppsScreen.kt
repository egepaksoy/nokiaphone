package com.egepaksoy.kiss.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.data.Apps
import com.egepaksoy.kiss.data.KissSettings
import com.egepaksoy.kiss.data.SystemApps
import com.egepaksoy.kiss.ui.Navigator
import com.egepaksoy.kiss.ui.PickerMode
import com.egepaksoy.kiss.ui.Screen
import com.egepaksoy.kiss.ui.common.TopBar
import com.egepaksoy.kiss.ui.theme.Kiss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Kamera, galeri, hesap makinesi, notlar ve izin verilen tüm uygulamalar. */
@Composable
fun AppsScreen(s: KissSettings, nav: Navigator) {
    val ctx = LocalContext.current
    val tick by KissApp.instance.resumeTick.collectAsState()
    val extra by produceState(emptyList(), s.allowedApps, s.messagingApps, tick) {
        value = withContext(Dispatchers.IO) { Apps.entries(ctx, s.messagingApps + s.allowedApps) }
    }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar("Uygulamalar", { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp).padding(top = 8.dp, bottom = 24.dp)) {
            val builtIn = buildList<Pair<String, () -> Unit>> {
                if (s.camera) add("Kamera" to { SystemApps.openCamera(ctx) })
                if (s.gallery) add("Galeri" to { SystemApps.openGallery(ctx) })
                if (s.calculator) add("Hesap makinesi" to { nav.push(Screen.Calculator) })
                if (s.notes) add("Notlar" to {
                    val pkg = s.notesPackage
                    if (pkg != null) Apps.launch(ctx, pkg) else nav.push(Screen.AppPicker(PickerMode.Notes))
                })
            }
            builtIn.forEach { (label, go) -> BigItem(label, size = 30.sp, onClick = go) }
            if (extra.isNotEmpty()) {
                if (builtIn.isNotEmpty()) Divider()
                extra.forEach { a -> BigItem(a.label, size = 30.sp) { Apps.launch(ctx, a.pkg) } }
            }
            if (builtIn.isEmpty() && extra.isEmpty()) {
                Text("Henüz uygulama yok. Ayarlar'dan ekleyebilirsin.", color = Kiss.colors.muted, modifier = Modifier.padding(top = 24.dp))
            }
        }
    }
}

@Composable
private fun Divider() {
    val c = Kiss.colors
    Row(Modifier.padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("EKLEDİKLERİN", fontSize = 11.sp, letterSpacing = 1.2.sp, color = c.muted, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(c.line))
    }
}

