package com.egepaksoy.kiss.data

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.widget.Toast
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Alarm {
    /** Bilinen saat uygulamaları; Samsung önce. */
    private val CLOCKS = listOf(
        "com.sec.android.app.clockpackage",
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.miui.clock",
        "com.coloros.alarmclock",
        "com.oneplus.deskclock",
        "com.huawei.deskclock",
    )

    fun next(ctx: Context): String? {
        val t = ctx.getSystemService(AlarmManager::class.java).nextAlarmClock?.triggerTime ?: return null
        val dt = Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault())
        val time = dt.format(DateTimeFormatter.ofPattern("HH:mm"))
        val today = LocalDate.now()
        return when (dt.toLocalDate()) {
            today -> time
            today.plusDays(1) -> "Yarın $time"
            else -> dt.format(DateTimeFormatter.ofPattern("EEE HH:mm", Locale.forLanguageTag("tr")))
        }
    }

    /** Telefonun saat uygulamasının paketi. */
    fun clockPackage(ctx: Context): String? {
        val pm = ctx.packageManager
        CLOCKS.firstOrNull { pm.getLaunchIntentForPackage(it) != null }?.let { return it }
        for (action in listOf(AlarmClock.ACTION_SHOW_ALARMS, AlarmClock.ACTION_SET_ALARM)) {
            runCatching { pm.queryIntentActivities(Intent(action), PackageManager.MATCH_ALL) }
                .getOrNull()?.map { it.activityInfo.packageName }
                ?.firstOrNull { it != "android" && pm.getLaunchIntentForPackage(it) != null }
                ?.let { return it }
        }
        return null
    }

    fun open(ctx: Context) {
        val pkg = clockPackage(ctx)
        // 1) Alarmlar sekmesi, 2) saat uygulamasının kendisi
        val tries = buildList {
            add(Intent(AlarmClock.ACTION_SHOW_ALARMS).apply { if (pkg != null) setPackage(pkg) })
            if (pkg != null) ctx.packageManager.getLaunchIntentForPackage(pkg)?.let(::add)
            add(Intent(AlarmClock.ACTION_SHOW_ALARMS))
        }
        for (i in tries) {
            if (runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
        Toast.makeText(ctx, "Saat uygulaması açılamadı", Toast.LENGTH_SHORT).show()
    }
}
