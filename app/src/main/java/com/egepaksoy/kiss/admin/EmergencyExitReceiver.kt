package com.egepaksoy.kiss.admin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.policy.Policy

/**
 * Acil çıkış. Yalnızca bilgisayardan adb ile tetiklenebilir (DUMP izni):
 *   adb shell am broadcast -a com.egepaksoy.kiss.EMERGENCY_EXIT -n com.egepaksoy.kiss/.admin.EmergencyExitReceiver
 * Cihaz sahipliğini de kaldırmak için sona ekle: --ez remove_owner true
 */
class EmergencyExitReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.w("KissPolicy", "Emergency exit requested")
        if (intent.getBooleanExtra("remove_owner", false)) {
            Policy.removeDeviceOwner(context)
        } else {
            KissApp.instance.settings.update { it.copy(strict = false) }
            Policy.releaseAll(context)
        }
    }
}
