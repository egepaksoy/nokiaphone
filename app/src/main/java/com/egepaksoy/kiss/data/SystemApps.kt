package com.egepaksoy.kiss.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.widget.Toast

/** Telefonun kendi kamera ve galeri uygulamaları (Samsung'da Kamera ve Galeri). */
object SystemApps {
    private val GALLERY_KNOWN = listOf("com.sec.android.gallery3d", "com.google.android.apps.photos", "com.android.gallery3d")

    private fun resolve(ctx: Context, intent: Intent): String? = runCatching {
        ctx.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    }.getOrNull()?.takeIf { it != "android" }

    private fun cameraIntent() = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)

    private fun galleryIntent() = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)

    fun cameraPackage(ctx: Context): String? = resolve(ctx, cameraIntent())

    fun galleryPackage(ctx: Context): String? =
        resolve(ctx, galleryIntent())
            ?: GALLERY_KNOWN.firstOrNull { ctx.packageManager.getLaunchIntentForPackage(it) != null }

    fun openCamera(ctx: Context) {
        val ok = runCatching { ctx.startActivity(cameraIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        if (!ok) Toast.makeText(ctx, "Kamera uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
    }

    fun openGallery(ctx: Context) {
        val ok = runCatching { ctx.startActivity(galleryIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        if (!ok) {
            val pkg = galleryPackage(ctx)
            if (pkg != null) Apps.launch(ctx, pkg)
            else Toast.makeText(ctx, "Galeri uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }
}
