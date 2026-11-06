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
            Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE, Manifest.permission.READ_MEDIA_IMAGES,
        )
        listOf("com.example.maps" to "Haritalar", "com.example.bank" to "Banka", "com.example.keep" to "Keep").forEach { (pkg, label) ->
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
                notesPackage = "com.example.keep", waitSeconds = 10)
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
        go(Screen.Dialer); shot("02-dialer")
        go(Screen.Messages); shot("03-messages")
        go(Screen.Thread(null, "+905321234567")); shot("04-thread")
        go(Screen.Gallery); shot("05-gallery")
        go(Screen.Calculator); shot("06-calculator")
        go(Screen.Gate); shot("07-gate")
        go(Screen.Settings); shot("08-settings")
        go(Screen.AppPicker(notes = false)); shot("09-apps")
        go(Screen.Strict); shot("10-strict")
        go(Screen.ChangePin); shot("11-pin")
        rule.runOnUiThread { KissApp.instance.settings.update { it.copy(camera = false, gallery = false, torch = false) } }
        go(Screen.Home); shot("12-home-minimal")
    }
}
