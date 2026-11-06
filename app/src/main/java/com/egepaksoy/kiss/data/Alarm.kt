package com.egepaksoy.kiss.data

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Alarm {
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

    fun open(ctx: Context) {
        val ok = runCatching {
            ctx.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (!ok) Toast.makeText(ctx, "Saat uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
    }
}
