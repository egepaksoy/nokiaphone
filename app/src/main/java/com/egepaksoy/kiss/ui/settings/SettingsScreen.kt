package com.egepaksoy.kiss.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.BuildConfig
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.data.AppEntry
import com.egepaksoy.kiss.data.Apps
import com.egepaksoy.kiss.data.KissSettings
import com.egepaksoy.kiss.policy.Policy
import com.egepaksoy.kiss.ui.Navigator
import com.egepaksoy.kiss.ui.PickerMode
import com.egepaksoy.kiss.ui.Screen
import com.egepaksoy.kiss.lock.ScreenLock
import com.egepaksoy.kiss.ui.common.Chevron
import com.egepaksoy.kiss.ui.common.Divider
import com.egepaksoy.kiss.ui.common.Group
import com.egepaksoy.kiss.ui.common.GroupRow
import com.egepaksoy.kiss.ui.common.KSwitch
import com.egepaksoy.kiss.ui.common.SectionLabel
import com.egepaksoy.kiss.ui.common.TopBar
import com.egepaksoy.kiss.ui.theme.Kiss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val WAIT_OPTIONS = listOf(5, 10, 30, 60, 120, 300)

fun waitLabel(s: Int) = if (s < 60) "$s sn" else "${s / 60} dk"

@Composable
fun SettingsScreen(s: KissSettings, nav: Navigator) {
    val ctx = LocalContext.current
    val c = Kiss.colors
    val store = KissApp.instance.settings
    val isOwner = remember { Policy.isDeviceOwner(ctx) }
    val allowed by produceState(emptyList<AppEntry>(), s.allowedApps) {
        value = withContext(Dispatchers.IO) { Apps.entries(ctx, s.allowedApps) }
    }
    val notesLabel = remember(s.notesPackage) { s.notesPackage?.let { Apps.label(ctx, it) } }
    val messengers by produceState(emptyList<AppEntry>(), s.messagingApps) {
        value = withContext(Dispatchers.IO) { Apps.entries(ctx, s.messagingApps) }
    }
    val tick by KissApp.instance.resumeTick.collectAsState()
    val lockService = remember(tick) { ScreenLock.serviceEnabled() }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar("Ayarlar", { nav.pop() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(bottom = 24.dp)) {
            SectionLabel("Sıkı mod")
            Group {
                GroupRow(
                    "Sıkı mod",
                    when {
                        !isOwner -> "Kurulmadı · bilgisayarla bir kerelik kurulum gerekir"
                        s.strict -> "Açık · izin listesi dışında hiçbir şey açılmaz"
                        else -> "Kapalı"
                    },
                    onClick = { nav.push(Screen.Strict) },
                    trailing = { Chevron() },
                )
            }

            SectionLabel("Her zaman açık")
            Group { GroupRow("Arama ve Mesajlar", "Kapatılamaz. Acil arama her zaman çalışır.") }

            SectionLabel("Özellikler")
            Group {
                Toggle("Kamera", "Telefonun kendi kamera uygulaması", s.camera) { v -> store.update { it.copy(camera = v) } }
                Divider()
                Toggle("Galeri", "Telefonun kendi galeri uygulaması", s.gallery) { v -> store.update { it.copy(gallery = v) } }
                Divider()
                Toggle("Fener", "Ana ekranda kısa yol", s.torch) { v -> store.update { it.copy(torch = v) } }
                Divider()
                Toggle("Alarm", "Saate dokununca Saat uygulaması açılır, sıradaki alarm tarihin yanında görünür", s.alarm) { v -> store.update { it.copy(alarm = v) } }
                Divider()
                Toggle("Hesap makinesi", null, s.calculator) { v -> store.update { it.copy(calculator = v) } }
                Divider()
                Toggle("Notlar", "Seçtiğin not uygulamasını açar", s.notes) { v -> store.update { it.copy(notes = v) } }
                if (s.notes) {
                    Divider()
                    GroupRow(
                        "Not uygulaması", notesLabel ?: "Seçilmedi",
                        onClick = { nav.push(Screen.AppPicker(PickerMode.Notes)) },
                        trailing = { Chevron() },
                    )
                }
            }

            SectionLabel("Mesajlar")
            Group {
                GroupRow("SMS", "Yerleşik, her zaman açık")
                messengers.forEach { a ->
                    Divider()
                    AppRow(a, true) { v -> if (!v) store.update { it.copy(messagingApps = it.messagingApps - a.pkg) } }
                }
                Divider()
                GroupRow(
                    "Mesaj uygulaması ekle", "Ör. WhatsApp. Telefon > Mesajlar'da kısayol olur, Uygulamalar'da görünür.",
                    titleColor = c.accent,
                    onClick = { nav.push(Screen.AppPicker(PickerMode.Messaging)) },
                    trailing = { Chevron() },
                )
            }

            SectionLabel("Ekran kilidi")
            Group {
                Toggle("Çift dokunarak kilitle", "Ana ekranda boş bir yere iki kez dokun", s.doubleTapLock) { v ->
                    store.update { it.copy(doubleTapLock = v) }
                }
                if (s.doubleTapLock) {
                    Divider()
                    GroupRow(
                        "Parmak izi ile açma",
                        if (lockService) "Açık · KISS ekran kilidi servisi çalışıyor"
                        else "Kapalı · kilitledikten sonra PIN gerekir. Açmak için Erişilebilirlik > KISS ekran kilidi",
                        onClick = { Policy.openTemporarily(ctx, ScreenLock.accessibilitySettings()) },
                        trailing = { Chevron() },
                    )
                }
            }

            SectionLabel("İzin verilen uygulamalar")
            Group {
                allowed.forEach { a ->
                    AppRow(a, true) { v -> if (!v) store.update { it.copy(allowedApps = it.allowedApps - a.pkg) } }
                    Divider()
                }
                GroupRow(
                    if (allowed.isEmpty()) "Uygulama ekle" else "Uygulamaları düzenle",
                    "Telefondaki her uygulama listelenir",
                    titleColor = c.accent,
                    onClick = { nav.push(Screen.AppPicker(PickerMode.Allowed)) },
                    trailing = { Chevron() },
                )
            }

            SectionLabel("Sistem")
            Group {
                SystemRow("Wi-Fi", Intent(Settings.Panel.ACTION_WIFI))
                Divider()
                SystemRow("İnternet ve uçak modu", Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                Divider()
                SystemRow("Bluetooth", Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                Divider()
                SystemRow("Ses", Intent(Settings.Panel.ACTION_VOLUME))
                Divider()
                SystemRow("Ekran ve parlaklık", Intent(Settings.ACTION_DISPLAY_SETTINGS))
                Divider()
                SystemRow("Tüm sistem ayarları", Intent(Settings.ACTION_SETTINGS))
            }
            Text(
                "Sıkı modda bu ekranlar sen KISS'e dönene kadar açık kalır.",
                color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 6.dp),
            )

            SectionLabel("Kilit")
            Group {
                GroupRow(
                    "Bekleme süresi", "Ayarlar açılmadan önce",
                    onClick = {
                        val i = WAIT_OPTIONS.indexOf(s.waitSeconds)
                        val next = WAIT_OPTIONS[(i + 1).mod(WAIT_OPTIONS.size)]
                        store.update { it.copy(waitSeconds = next) }
                    },
                    trailing = { Text(waitLabel(s.waitSeconds), color = c.muted) },
                )
                Divider()
                GroupRow("PIN'i değiştir", onClick = { nav.push(Screen.ChangePin) }, trailing = { Chevron() })
            }

            Spacer(Modifier.height(18.dp))
            Text("KISS ${BuildConfig.VERSION_NAME} · Keep It Seriously Simple", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun Toggle(title: String, subtitle: String?, value: Boolean, onChange: (Boolean) -> Unit) {
    GroupRow(title, subtitle, onClick = { onChange(!value) }, trailing = { KSwitch(value, onChange) })
}

@Composable
private fun SystemRow(title: String, intent: Intent) {
    val ctx = LocalContext.current
    GroupRow(title, onClick = { Policy.openTemporarily(ctx, intent) }, trailing = { Chevron() })
}

@Composable
fun AppRow(a: AppEntry, checked: Boolean, single: Boolean = false, onChange: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val icon = remember(a.pkg) { Apps.icon(ctx, a.pkg) }
    GroupRow(
        a.label,
        onClick = { onChange(!checked) },
        leading = {
            if (icon != null) Image(icon.asImageBitmap(), null, Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)))
        },
        trailing = {
            if (single) { if (checked) androidx.compose.material3.Icon(com.egepaksoy.kiss.ui.common.KIcons.Check, null, tint = Kiss.colors.accent) }
            else KSwitch(checked, onChange)
        },
    )
}
