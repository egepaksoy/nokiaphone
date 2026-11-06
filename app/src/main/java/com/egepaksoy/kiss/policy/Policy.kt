package com.egepaksoy.kiss.policy

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.MainActivity
import com.egepaksoy.kiss.admin.KissAdminReceiver
import com.egepaksoy.kiss.data.KissSettings
import com.egepaksoy.kiss.data.SystemApps

/**
 * Sıkı mod. KISS cihaz sahibi (Device Owner) olduğunda:
 *  - KISS kalıcı ana ekran yapılır,
 *  - Lock Task (kiosk) modu yalnızca izin verilen paketlerle çalışır,
 *  - gizli uygulamalar isteğe bağlı olarak askıya alınır (bildirimleri de gizlenir).
 */
object Policy {
    private const val TAG = "KissPolicy"

    /** Arama, alarm ve izin pencereleri gibi her zaman çalışması gereken sistem paketleri. */
    private val KNOWN_ESSENTIAL = listOf(
        "com.samsung.android.incallui",
        "com.samsung.android.dialer",
        "com.android.server.telecom",
        "com.android.phone",
        "com.android.incallui",
        "com.google.android.dialer",
        "com.sec.android.app.clockpackage",
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.providers.media.module",
        "com.google.android.providers.media.module",
        "com.samsung.android.app.telephonyui",
        "com.android.stk",
    )

    /** Askıya alınmaması gereken ek paketler (güncellemeler, kişiler, ana ekranlar). */
    private val NEVER_SUSPEND = listOf(
        "com.android.settings",
        "com.android.vending",
        "com.sec.android.app.samsungapps",
        "com.samsung.android.app.contacts",
        "com.android.contacts",
        "com.google.android.contacts",
        "com.samsung.android.messaging",
        "com.google.android.apps.messaging",
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.gms",
        "com.samsung.android.lool",
    )

    val OWN_PERMISSIONS = buildList {
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.READ_CALL_LOG)
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun admin(ctx: Context) = ComponentName(ctx, KissAdminReceiver::class.java)
    private fun dpm(ctx: Context) = ctx.getSystemService(DevicePolicyManager::class.java)

    fun isDeviceOwner(ctx: Context): Boolean =
        runCatching { dpm(ctx).isDeviceOwnerApp(ctx.packageName) }.getOrDefault(false)

    fun isStrictActive(ctx: Context): Boolean =
        KissApp.instance.settings.value.strict && isDeviceOwner(ctx)

    private fun installed(pm: PackageManager, pkg: String) =
        runCatching { pm.getPackageInfo(pkg, 0); true }.getOrDefault(false)

    private fun resolvePackage(pm: PackageManager, intent: Intent): String? =
        runCatching { pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName }
            .getOrNull()?.takeIf { it != "android" }

    fun clockPackage(ctx: Context): String? =
        resolvePackage(ctx.packageManager, Intent(AlarmClock.ACTION_SHOW_ALARMS))
            ?: resolvePackage(ctx.packageManager, Intent(AlarmClock.ACTION_SET_ALARM))

    fun settingsPackage(ctx: Context): String =
        resolvePackage(ctx.packageManager, Intent(Settings.ACTION_SETTINGS)) ?: "com.android.settings"

    private fun essentialPackages(ctx: Context): Set<String> {
        val pm = ctx.packageManager
        val set = linkedSetOf(ctx.packageName)
        runCatching { ctx.getSystemService(TelecomManager::class.java).defaultDialerPackage }
            .getOrNull()?.let(set::add)
        clockPackage(ctx)?.let(set::add)
        KNOWN_ESSENTIAL.filterTo(set) { installed(pm, it) }
        return set
    }

    /** Lock Task modunda açılabilecek paketler. */
    fun lockTaskPackages(ctx: Context, s: KissSettings): Set<String> {
        val set = essentialPackages(ctx).toMutableSet()
        set += s.allowedApps
        if (s.notes) s.notesPackage?.let(set::add)
        set += s.messagingApps
        if (s.camera) SystemApps.cameraPackage(ctx)?.let(set::add)
        if (s.gallery) SystemApps.galleryPackage(ctx)?.let(set::add)
        set += KissApp.instance.settings.temporaryPackages
        return set
    }

    private fun launchablePackages(ctx: Context): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return ctx.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }.toSet()
    }

    private fun suspendTargets(ctx: Context, s: KissSettings): Set<String> {
        val keep = lockTaskPackages(ctx, s).toMutableSet()
        keep += settingsPackage(ctx)
        keep += NEVER_SUSPEND
        // Varsayılan SMS uygulaması gelen mesajları depoya yazar; askıya alınmamalı.
        // Kiosk listesinde değil, yani arayüzü açılamaz; mesajlar KISS'te okunur.
        runCatching { Telephony.Sms.getDefaultSmsPackage(ctx) }.getOrNull()?.let { keep += it }
        runCatching {
            ctx.getSystemService(InputMethodManager::class.java).enabledInputMethodList
                .forEach { keep += it.packageName }
        }
        return launchablePackages(ctx) - keep
    }

    /** Ayarları cihaza uygular. Cihaz sahibi değilse hiçbir şey yapmaz. */
    @Synchronized
    fun apply(ctx: Context, s: KissSettings = KissApp.instance.settings.value) {
        if (!isDeviceOwner(ctx)) return
        val dpm = dpm(ctx)
        val admin = admin(ctx)
        val store = KissApp.instance.settings
        grantOwnPermissions(ctx)
        try {
            if (s.strict) {
                val home = IntentFilter(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addCategory(Intent.CATEGORY_DEFAULT)
                }
                dpm.addPersistentPreferredActivity(admin, home, ComponentName(ctx, MainActivity::class.java))
                dpm.setLockTaskPackages(admin, lockTaskPackages(ctx, s).toTypedArray())
                var features = DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO or
                    DevicePolicyManager.LOCK_TASK_FEATURE_HOME or
                    DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS or
                    DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD
                if (s.notificationPanel) features = features or DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS
                dpm.setLockTaskFeatures(admin, features)

                val previous = store.suspendedPackages
                val targets = if (s.suspendHidden) suspendTargets(ctx, s) else emptySet()
                val release = previous - targets
                if (release.isNotEmpty()) dpm.setPackagesSuspended(admin, release.toTypedArray(), false)
                val failed = if (targets.isNotEmpty())
                    dpm.setPackagesSuspended(admin, targets.toTypedArray(), true).toSet() else emptySet()
                store.suspendedPackages = targets - failed
            } else {
                releaseAll(ctx)
            }
        } catch (e: Exception) {
            Log.w(TAG, "apply failed", e)
        }
    }

    /** Sıkı modun tüm etkilerini kaldırır (cihaz sahipliği kalır). */
    fun releaseAll(ctx: Context) {
        if (!isDeviceOwner(ctx)) return
        val dpm = dpm(ctx)
        val admin = admin(ctx)
        val store = KissApp.instance.settings
        runCatching {
            val prev = store.suspendedPackages
            if (prev.isNotEmpty()) dpm.setPackagesSuspended(admin, prev.toTypedArray(), false)
            store.suspendedPackages = emptySet()
        }
        runCatching { dpm.clearPackagePersistentPreferredActivities(admin, ctx.packageName) }
        runCatching { dpm.setLockTaskPackages(admin, emptyArray()) }
        store.temporaryPackages = emptySet()
    }

    /** Sıkı modu kapatır ve KISS'in cihaz sahipliğini kaldırır. Sonra uygulama normal şekilde silinebilir. */
    fun removeDeviceOwner(ctx: Context) {
        KissApp.instance.settings.update { it.copy(strict = false) }
        releaseAll(ctx)
        @Suppress("DEPRECATION")
        runCatching { dpm(ctx).clearDeviceOwnerApp(ctx.packageName) }
    }

    fun grantOwnPermissions(ctx: Context) {
        if (!isDeviceOwner(ctx)) return
        val dpm = dpm(ctx)
        val admin = admin(ctx)
        OWN_PERMISSIONS.forEach { p ->
            if (ctx.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                runCatching {
                    dpm.setPermissionGrantState(admin, ctx.packageName, p,
                        DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED)
                }
            }
        }
    }

    /** Sıkı modda bir sistem ekranını (ör. Wi-Fi) KISS'e dönene kadar açar. */
    fun openTemporarily(ctx: Context, intent: Intent, pkg: String? = null) {
        val target = pkg ?: resolvePackage(ctx.packageManager, intent) ?: settingsPackage(ctx)
        if (isStrictActive(ctx)) {
            val store = KissApp.instance.settings
            store.temporaryPackages = store.temporaryPackages + target
            apply(ctx)
        }
        runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** KISS öne geldiğinde geçici izinleri geri alır. */
    fun revokeTemporary(ctx: Context): Boolean {
        val store = KissApp.instance.settings
        if (store.temporaryPackages.isEmpty()) return false
        store.temporaryPackages = emptySet()
        return true
    }
}
