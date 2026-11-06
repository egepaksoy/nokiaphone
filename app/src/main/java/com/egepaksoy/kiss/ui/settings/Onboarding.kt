package com.egepaksoy.kiss.ui.settings

import android.app.role.RoleManager
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.data.Pin
import com.egepaksoy.kiss.policy.Policy
import com.egepaksoy.kiss.ui.Navigator
import com.egepaksoy.kiss.ui.common.PrimaryButton
import com.egepaksoy.kiss.ui.common.TopBar
import com.egepaksoy.kiss.ui.theme.Display
import com.egepaksoy.kiss.ui.theme.Kiss

private enum class Step { Welcome, Pin, Confirm, Permissions, Home }

@Composable
fun OnboardingScreen() {
    val ctx = LocalContext.current
    val c = Kiss.colors
    val store = KissApp.instance.settings
    var step by rememberSaveable { mutableStateOf(Step.Welcome) }
    var first by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        step = Step.Home
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        store.update { it.copy(onboarded = true) }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        when (step) {
            Step.Welcome -> Intro(
                "KISS", "Keep It Seriously Simple.\n\nTelefonun artık arama, mesaj ve seçtiğin birkaç şeyden ibaret. " +
                    "Önce ayarları koruyacak 4 haneli bir PIN belirleyelim.",
                "Başla",
            ) { step = Step.Pin }
            Step.Pin -> PinEntry("PIN belirle", "Ayarlara girerken sorulacak", error) { p ->
                first = p; error = null; step = Step.Confirm; true
            }
            Step.Confirm -> PinEntry("PIN'i tekrar gir", "Doğrulamak için", error) { p ->
                if (p == first) {
                    store.update { it.copy(pin = Pin.hash(p)) }
                    error = null
                    step = if (Policy.isDeviceOwner(ctx)) { Policy.grantOwnPermissions(ctx); Step.Home } else Step.Permissions
                    true
                } else { error = "PIN'ler aynı değil"; step = Step.Pin; false }
            }
            Step.Permissions -> Intro(
                "İzinler", "Arama yapmak, son aramaları ve mesajları göstermek için izin gerekiyor. " +
                    "Veriler telefondan çıkmaz; KISS internete bağlanmaz.",
                "İzinleri ver",
            ) { permLauncher.launch(Policy.OWN_PERMISSIONS.toTypedArray()) }
            Step.Home -> Intro(
                "Ana ekran", "KISS'i varsayılan ana ekran yap. Ana ekran tuşuna her bastığında KISS açılır.\n\n" +
                    "Sıkı modu daha sonra Ayarlar > Sıkı mod'dan açabilirsin.",
                "Ana ekran yap",
            ) {
                val rm = ctx.getSystemService(RoleManager::class.java)
                if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                    runCatching { roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)) }
                        .onFailure {
                            runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                            store.update { it.copy(onboarded = true) }
                        }
                } else store.update { it.copy(onboarded = true) }
            }
        }
    }
}

@Composable
private fun Intro(title: String, body: String, button: String, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        Spacer(Modifier.weight(1f))
        Text(title, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 52.sp, letterSpacing = (-0.03).em, lineHeight = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(body, color = Kiss.colors.muted, fontSize = 17.sp, lineHeight = 25.sp)
        Spacer(Modifier.weight(1.4f))
        PrimaryButton(button, onNext, Modifier.fillMaxWidth())
    }
}

@Composable
fun ChangePinScreen(nav: Navigator) {
    val store = KissApp.instance.settings
    var stage by rememberSaveable { mutableStateOf(0) }
    var first by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar("PIN'i değiştir", { nav.pop() })
        when (stage) {
            0 -> PinEntry("Mevcut PIN", "Önce şu anki PIN'i gir", error) { p ->
                if (Pin.verify(p, store.value.pin)) { stage = 1; error = null; true } else { error = "PIN yanlış"; false }
            }
            1 -> PinEntry("Yeni PIN", "4 hane", error) { p -> first = p; stage = 2; error = null; true }
            else -> PinEntry("Yeni PIN'i tekrar gir", "Doğrulamak için", error) { p ->
                if (p == first) { store.update { it.copy(pin = Pin.hash(p)) }; nav.pop(); true }
                else { error = "PIN'ler aynı değil"; stage = 1; false }
            }
        }
    }
}

