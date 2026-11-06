package com.egepaksoy.kiss.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.widget.Toast

/**
 * Telefonun kendi kamera ve galeri uygulamaları (Samsung'da Kamera ve Galeri).
 * Android'in "varsayılan" önerisi Google Fotoğraflar olabildiği için önce üreticinin
 * kendi uygulamaları aranır, sonra sistemle gelen uygulamalar, en son herhangi biri.
 */
object SystemApps {
    private val GALLERY_OEM = listOf(
        "com.sec.android.gallery3d",      // Samsung
        "com.miui.gallery",               // Xiaomi
        "com.coloros.gallery3d",          // Oppo / Realme
        "com.oneplus.gallery",
        "com.huawei.photos",
        "com.android.gallery3d",
    )
    private val CAMERA_OEM = listOf(
        "com.sec.android.app.camera",     // Samsung
        "com.android.camera",
        "com.oplus.camera",
        "com.huawei.camera",
        "com.android.camera2",
    )
    private const val GOOGLE_PHOTOS = "com.google.android.apps.photos"

    private fun launchable(ctx: Context, pkg: String) = ctx.packageManager.getLaunchIntentForPackage(pkg) != null

    private fun isSystem(ctx: Context, pkg: String) = runCatching {
        ctx.packageManager.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM != 0
    }.getOrDefault(false)

    private fun handlers(ctx: Context, intent: Intent): List<String> = runCatching {
        ctx.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }.distinct()
    }.getOrDefault(emptyList())

    private fun pick(ctx: Context, oem: List<String>, intent: Intent, avoid: Set<String> = emptySet()): String? {
        oem.firstOrNull { launchable(ctx, it) }?.let { return it }
        val all = handlers(ctx, intent).filter { launchable(ctx, it) }
        return all.firstOrNull { isSystem(ctx, it) && it !in avoid }
            ?: all.firstOrNull { it !in avoid }
            ?: all.firstOrNull()
    }

    fun cameraPackage(ctx: Context): String? =
        pick(ctx, CAMERA_OEM, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))

    fun galleryPackage(ctx: Context): String? =
        pick(
            ctx, GALLERY_OEM,
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY),
            avoid = setOf(GOOGLE_PHOTOS),
        ) ?: GOOGLE_PHOTOS.takeIf { launchable(ctx, it) }

    /** KISS dışındaki ana ekran uygulaması (Samsung'da One UI Home). */
    fun otherLauncher(ctx: Context): android.content.ComponentName? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val list = runCatching { ctx.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL) }
            .getOrDefault(emptyList())
            .map { it.activityInfo }
            .filter { it.packageName != ctx.packageName && it.packageName != "com.android.settings" }
        val best = list.firstOrNull { it.packageName == "com.sec.android.app.launcher" }
            ?: list.firstOrNull { isSystem(ctx, it.packageName) }
            ?: list.firstOrNull()
        return best?.let { android.content.ComponentName(it.packageName, it.name) }
    }

    /** Telefonun normal ana ekranını açar. Açılamazsa false döner. */
    fun openOtherLauncher(ctx: Context): Boolean {
        val cn = otherLauncher(ctx) ?: return false
        return runCatching {
            ctx.startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(cn)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
            )
        }.isSuccess
    }

    fun openCamera(ctx: Context) {
        val pkg = cameraPackage(ctx)
        val intent = if (pkg != null) Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).setPackage(pkg)
        else Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        val ok = runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        if (!ok) {
            if (pkg != null) Apps.launch(ctx, pkg)
            else Toast.makeText(ctx, "Kamera uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    fun openGallery(ctx: Context) {
        val pkg = galleryPackage(ctx)
        if (pkg != null) Apps.launch(ctx, pkg)
        else Toast.makeText(ctx, "Galeri uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
    }
}
