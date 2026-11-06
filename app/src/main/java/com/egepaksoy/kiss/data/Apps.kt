package com.egepaksoy.kiss.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(val pkg: String, val label: String)

object Apps {
    fun launchable(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .asSequence()
            .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != ctx.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun label(ctx: Context, pkg: String): String? = runCatching {
        val pm = ctx.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrNull()

    fun entries(ctx: Context, pkgs: Collection<String>): List<AppEntry> =
        pkgs.mapNotNull { p -> label(ctx, p)?.let { AppEntry(p, it) } }.sortedBy { it.label.lowercase() }

    private val iconCache = HashMap<String, Bitmap>()

    fun icon(ctx: Context, pkg: String): Bitmap? = synchronized(iconCache) {
        iconCache[pkg] ?: runCatching {
            ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96)
        }.getOrNull()?.also { iconCache[pkg] = it }
    }

    fun launch(ctx: Context, pkg: String) {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            Toast.makeText(ctx, "Uygulama bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Toast.makeText(ctx, "Uygulama açılamadı", Toast.LENGTH_SHORT).show() }
    }
}
