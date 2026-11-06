package com.egepaksoy.kiss.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.MainActivity
import com.egepaksoy.kiss.data.KissSettings
import com.egepaksoy.kiss.policy.Policy
import com.egepaksoy.kiss.ui.CountdownAction
import com.egepaksoy.kiss.ui.Navigator
import com.egepaksoy.kiss.ui.Screen
import com.egepaksoy.kiss.ui.common.Chevron
import com.egepaksoy.kiss.ui.common.Divider
import com.egepaksoy.kiss.ui.common.Group
import com.egepaksoy.kiss.ui.common.GroupRow
import com.egepaksoy.kiss.ui.common.KSwitch
import com.egepaksoy.kiss.ui.common.PrimaryButton
import com.egepaksoy.kiss.ui.common.SectionLabel
import com.egepaksoy.kiss.ui.common.Ticker
import com.egepaksoy.kiss.ui.common.TopBar
import com.egepaksoy.kiss.ui.theme.Display
import com.egepaksoy.kiss.ui.theme.Kiss

const val OWNER_CMD = "adb shell dpm set-device-owner com.egepaksoy.kiss/.admin.KissAdminReceiver"
const val EXIT_CMD = "adb shell am broadcast -a com.egepaksoy.kiss.EMERGENCY_EXIT -n com.egepaksoy.kiss/.admin.EmergencyExitReceiver"
const val EXIT_WAIT = 120

@Composable
fun StrictScreen(s: KissSettings, nav: Navigator) {
    val ctx = LocalContext.current
    val c = Kiss.colors
    val store = KissApp.instance.settings
    val tick by KissApp.instance.resumeTick.collectAsState()
    val owner = remember(tick) { Policy.isDeviceOwner(ctx) }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar("Sıkı mod", { nav.pop() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(bottom = 28.dp)) {
            Text(
                "Sıkı modda telefon kiosk moduna geçer: yalnızca KISS ve izin verdiğin uygulamalar açılır. " +
                    "Son uygulamalar ekranı kapanır, gizli uygulamalar askıya alınır ve bildirim gönderemez. " +
                    "Gelen aramalar, alarmlar ve acil arama çalışmaya devam eder.",
                color = c.muted, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            if (!owner) {
                SectionLabel("Kurulum (bir kerelik)")
                Group {
                    Step(1, "Telefonda Geliştirici seçenekleri > USB hata ayıklama'yı aç. Samsung'da Ayarlar > Güvenlik > Otomatik Engelleyici kapalı olmalı.")
                    Divider()
                    Step(2, "Ayarlar > Hesaplar'dan tüm hesapları (Samsung, Google, WhatsApp vb.) geçici olarak kaldır. Güvenli Klasör ve Dual Messenger varsa kapat.")
                    Divider()
                    Step(3, "Telefonu bilgisayara bağla ve şu komutu çalıştır:")
                    Command(OWNER_CMD)
                    Divider()
                    Step(4, "\"Success\" yazısını görünce hesaplarını geri ekle. Sonra bu ekrana dön ve sıkı modu aç.")
                }
                Text(
                    "Ayrıntılı rehber: proje deposundaki docs/KURULUM.md",
                    color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                )
            } else {
                SectionLabel("Durum")
                Group {
                    GroupRow(
                        "Sıkı mod",
                        if (s.strict) "Açık" else "Kapalı",
                        onClick = {
                            if (s.strict) nav.push(Screen.Countdown(CountdownAction.ExitStrict))
                            else store.update { it.copy(strict = true, enabled = true) }
                        },
                        trailing = {
                            KSwitch(s.strict, { v ->
                                if (v) store.update { it.copy(strict = true, enabled = true) }
                                else nav.push(Screen.Countdown(CountdownAction.ExitStrict))
                            })
                        },
                    )
                }
                SectionLabel("Seçenekler")
                Group {
                    GroupRow(
                        "Bildirim paneli ve hızlı ayarlar",
                        "Wi-Fi, parlaklık, rahatsız etme gibi kısayollar için",
                        onClick = { store.update { it.copy(notificationPanel = !it.notificationPanel) } },
                        trailing = { KSwitch(s.notificationPanel, { v -> store.update { it.copy(notificationPanel = v) } }) },
                    )
                    Divider()
                    GroupRow(
                        "Gizli uygulamaları askıya al",
                        "Bildirimleri de gizlenir. Sıkı mod kapanınca geri gelir.",
                        onClick = { store.update { it.copy(suspendHidden = !it.suspendHidden) } },
                        trailing = { KSwitch(s.suspendHidden, { v -> store.update { it.copy(suspendHidden = v) } }) },
                    )
                }
                SectionLabel("Acil çıkış")
                Group {
                    GroupRow("Bir şeyler ters giderse bilgisayardan:", null)
                    Command(EXIT_CMD)
                }
                SectionLabel("Kaldırma")
                Group {
                    GroupRow(
                        "Cihaz sahipliğini kaldır", "KISS'i silebilmek için gerekir",
                        titleColor = c.end,
                        onClick = { nav.push(Screen.Countdown(CountdownAction.RemoveOwner)) },
                        trailing = { Chevron() },
                    )
                }
            }
        }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("$n", fontFamily = Display, fontWeight = FontWeight.SemiBold, color = Kiss.colors.accent, fontSize = 18.sp)
        Text(text, fontSize = 15.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun Command(cmd: String) {
    val ctx = LocalContext.current
    val c = Kiss.colors
    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.bg).padding(12.dp)) {
            SelectionContainer { Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp) }
        }
        Text(
            "Kopyala", color = c.accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).clickable {
                ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("komut", cmd))
                Toast.makeText(ctx, "Kopyalandı", Toast.LENGTH_SHORT).show()
            }.padding(vertical = 4.dp),
        )
    }
}

@Composable
fun CountdownScreen(action: CountdownAction, nav: Navigator) {
    val ctx = LocalContext.current
    var left by remember { mutableIntStateOf(EXIT_WAIT) }
    Ticker(EXIT_WAIT) { left = it }
    val title = if (action == CountdownAction.ExitStrict) "Sıkı modu kapat" else "Cihaz sahipliğini kaldır"
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar(title, { nav.pop() })
        Box(Modifier.weight(1f)) {
            WaitRing(
                left, EXIT_WAIT,
                if (left > 0) "Acele etme" else "Hazır",
                if (action == CountdownAction.ExitStrict)
                    "Sıkı mod kapanınca telefon normale döner. Kararın hâlâ aynıysa süre dolunca onayla."
                else "KISS'in yönetici yetkisi silinir. Sıkı modu tekrar açmak için bilgisayarla yeniden kurulum gerekir.",
            )
        }
        PrimaryButton(
            if (left > 0) "Bekle…" else title,
            {
                if (action == CountdownAction.ExitStrict) {
                    KissApp.instance.settings.update { it.copy(strict = false) }
                    Policy.releaseAll(ctx)
                } else {
                    Policy.removeDeviceOwner(ctx)
                }
                (ctx as? MainActivity)?.enforceLockTask()
                nav.home()
            },
            Modifier.fillMaxWidth().padding(20.dp),
            enabled = left == 0,
            color = Kiss.colors.end,
        )
        Spacer(Modifier.height(8.dp))
    }
}
