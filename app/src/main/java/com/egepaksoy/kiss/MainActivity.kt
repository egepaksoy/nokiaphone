package com.egepaksoy.kiss

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.egepaksoy.kiss.policy.Policy
import com.egepaksoy.kiss.ui.KissRoot
import com.egepaksoy.kiss.ui.Screen
import com.egepaksoy.kiss.ui.theme.KissTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val app get() = KissApp.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KissTheme { KissRoot() } }
        handleIntent(intent, fresh = true)

        // Ayar her değiştiğinde politikayı uygula ve kiosk durumunu güncelle.
        lifecycleScope.launch {
            app.settings.state.drop(1).collect { s ->
                withContext(Dispatchers.Default) { Policy.apply(this@MainActivity, s) }
                enforceLockTask()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdge() // açık/koyu tema değişince durum çubuğu simgelerini güncelle
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent, fresh = false)
    }

    private fun handleIntent(intent: Intent?, fresh: Boolean) {
        val nav = app.nav
        when {
            intent?.action == ACTION_OPEN_THREAD -> {
                val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return
                nav.home()
                nav.push(Screen.Messages)
                nav.push(Screen.Thread(null, address))
            }
            !fresh && intent?.hasCategory(Intent.CATEGORY_HOME) == true -> nav.home()
        }
    }

    override fun onResume() {
        super.onResume()
        val ctx = this
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                Policy.revokeTemporary(ctx)
                Policy.apply(ctx)
            }
            enforceLockTask()
        }
        com.egepaksoy.kiss.data.Phone.clearCache()
        app.resumeTick.value++
    }

    fun enforceLockTask() {
        val am = getSystemService(ActivityManager::class.java)
        val state = am.lockTaskModeState
        if (Policy.isStrictActive(this)) {
            if (state == ActivityManager.LOCK_TASK_MODE_NONE) runCatching { startLockTask() }
        } else if (state != ActivityManager.LOCK_TASK_MODE_NONE) {
            runCatching { stopLockTask() }
        }
    }

    companion object {
        const val ACTION_OPEN_THREAD = "com.egepaksoy.kiss.OPEN_THREAD"
        const val EXTRA_ADDRESS = "address"
    }
}
