package com.egepaksoy.kiss.sms

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.MainActivity
import com.egepaksoy.kiss.R
import com.egepaksoy.kiss.data.Phone

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!KissApp.instance.settings.value.enabled) return // KISS kapalıyken bildirimi telefonun mesaj uygulaması gösterir
        val parts = runCatching { Telephony.Sms.Intents.getMessagesFromIntent(intent) }.getOrNull() ?: return
        if (parts.isEmpty()) return
        val address = parts[0].displayOriginatingAddress ?: parts[0].originatingAddress ?: return
        val body = parts.joinToString("") { it.displayMessageBody ?: "" }
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val name = Phone.nameFor(context, address) ?: address

        val open = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_THREAD)
            .putExtra(MainActivity.EXTRA_ADDRESS, address)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(context, address.hashCode(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, KissApp.CHANNEL_SMS)
            .setSmallIcon(R.drawable.ic_stat_kiss)
            .setContentTitle(name)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(address.hashCode(), n)
    }
}
