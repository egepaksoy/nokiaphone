package com.egepaksoy.kiss

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.egepaksoy.kiss.data.SettingsStore
import com.egepaksoy.kiss.data.Torch
import com.egepaksoy.kiss.ui.Navigator
import kotlinx.coroutines.flow.MutableStateFlow

class KissApp : Application() {
    lateinit var settings: SettingsStore
        private set
    lateinit var torch: Torch
        private set
    val nav = Navigator()

    /** Activity her öne geldiğinde artar; ekranlar veriyi tazelemek için dinler. */
    val resumeTick = MutableStateFlow(0)

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsStore(this)
        torch = Torch(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_SMS, "Mesajlar", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    companion object {
        const val CHANNEL_SMS = "sms"
        lateinit var instance: KissApp
            private set
    }
}
