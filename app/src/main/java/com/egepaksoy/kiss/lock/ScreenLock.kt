package com.egepaksoy.kiss.lock

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.egepaksoy.kiss.policy.Policy

/**
 * Ekranı kilitler. Önce erişilebilirlik servisini dener (parmak izi/yüz ile açma çalışmaya devam eder),
 * yoksa yönetici kilidini kullanır (Android bu durumda bir sonraki açılışta PIN ister).
 */
object ScreenLock {
    fun lock(activity: Activity) {
        if (LockService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) == true) return
        val dpm = activity.getSystemService(DevicePolicyManager::class.java)
        val admin = Policy.admin(activity)
        if (dpm.isAdminActive(admin)) {
            runCatching { dpm.lockNow() }
        } else {
            // Normal modda: bir kerelik yönetici izni iste.
            val i = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Ana ekranda boş bir yere iki kez dokunarak ekranı kilitleyebilmek için.")
            runCatching { activity.startActivity(i) }
        }
    }

    fun serviceEnabled(): Boolean = LockService.instance != null

    fun accessibilitySettings() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun adminActive(ctx: Context): Boolean =
        ctx.getSystemService(DevicePolicyManager::class.java).isAdminActive(Policy.admin(ctx))
}

/** Yalnızca ekranı kilitlemek için. Ekran içeriğini okumaz, olay dinlemez. */
class LockService : AccessibilityService() {
    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onDestroy() { instance = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    companion object {
        @Volatile var instance: LockService? = null
    }
}
