package com.egepaksoy.kiss

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.egepaksoy.kiss.data.Pin
import com.egepaksoy.kiss.ui.Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Gerçek MainActivity'yi Robolectric'te açar, ekranları gezer ve görüntülerini kaydeder. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreensTest {
    init {
        val app = ApplicationProvider.getApplicationContext<KissApp>()
        shadowOf(app).grantPermissions(
            Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE,
        )
        listOf("com.example.maps" to "Haritalar", "com.example.bank" to "Banka", "com.example.keep" to "Keep", "com.whatsapp" to "WhatsApp").forEach { (pkg, label) ->
            val ai = ApplicationInfo().apply { packageName = pkg; nonLocalizedLabel = label; name = label }
            val pi = PackageInfo().apply { packageName = pkg; applicationInfo = ai }
            shadowOf(app.packageManager).installPackage(pi)
            val act = ActivityInfo().apply { packageName = pkg; name = "$pkg.Main"; applicationInfo = ai; nonLocalizedLabel = label; exported = true }
            shadowOf(app.packageManager).addOrUpdateActivity(act)
            shadowOf(app.packageManager).addIntentFilterForActivity(
                ComponentName(pkg, "$pkg.Main"),
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            )
        }
        app.settings.update {
            it.copy(onboarded = true, pin = Pin.hash("1234"), allowedApps = setOf("com.example.maps", "com.example.bank"),
                notesPackage = "com.example.keep", waitSeconds = 10, messagingApps = setOf("com.whatsapp"))
        }
    }

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val out = File(System.getProperty("kiss.shots") ?: "build/shots").apply { mkdirs() }

    private fun shot(name: String) {
        rule.mainClock.autoAdvance = false
        repeat(8) {
            rule.mainClock.advanceTimeBy(250)
            Thread.sleep(60)
            shadowOf(android.os.Looper.getMainLooper()).idle()
        }
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(android.graphics.Canvas(bmp)) }
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun go(s: Screen) {
        rule.runOnUiThread { KissApp.instance.nav.home(); if (s != Screen.Home) KissApp.instance.nav.push(s) }
    }

    @Test fun screens() {
        shot("01-home")
        go(Screen.AppsList); shot("02-apps")
        go(Screen.Phone(0)); shot("03-phone-keys")
        go(Screen.Phone(1)); shot("04-phone-recent")
        go(Screen.Phone(2)); shot("05-phone-messages")
        go(Screen.Phone(3)); shot("06-phone-contacts")
        go(Screen.Thread(null, "+905321234567")); shot("07-thread")
        go(Screen.Calculator); shot("08-calculator")
        go(Screen.Gate); shot("09-gate")
        go(Screen.Settings); shot("10-settings")
        go(Screen.Strict); shot("11-strict")
    }
}
