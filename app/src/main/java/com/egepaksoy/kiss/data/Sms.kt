package com.egepaksoy.kiss.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsManager
import com.egepaksoy.kiss.KissApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class SmsThread(
    val threadId: Long,
    val address: String,
    val name: String?,
    val snippet: String,
    val date: Long,
    val fromMe: Boolean,
    val unread: Boolean,
) {
    val title get() = name ?: address
}

data class SmsMessage(val id: Long, val body: String, val date: Long, val fromMe: Boolean, val failed: Boolean)

object Sms {
    private const val INBOX = Telephony.Sms.MESSAGE_TYPE_INBOX

    fun threads(ctx: Context, maxRows: Int = 6000): List<SmsThread> {
        val store = KissApp.instance.settings
        val map = LinkedHashMap<Long, SmsThread>()
        val unread = HashSet<Long>()
        runCatching {
            ctx.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY,
                    Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.READ),
                null, null, Telephony.Sms.DATE + " DESC",
            )?.use { c ->
                var n = 0
                while (c.moveToNext() && n++ < maxRows) {
                    val tid = c.getLong(0)
                    val type = c.getInt(4)
                    val date = c.getLong(3)
                    if (type == INBOX && c.getInt(5) == 0 && date > store.threadSeen(tid)) unread += tid
                    if (!map.containsKey(tid)) {
                        val addr = c.getString(1) ?: continue
                        map[tid] = SmsThread(tid, addr, null, c.getString(2) ?: "", date, type != INBOX, false)
                    }
                }
            }
        }
        return map.values.map { t ->
            t.copy(name = Phone.nameFor(ctx, t.address), unread = t.threadId in unread)
        }
    }

    fun unreadCount(ctx: Context): Int {
        val store = KissApp.instance.settings
        val set = HashSet<Long>()
        runCatching {
            ctx.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.THREAD_ID, Telephony.Sms.DATE),
                "${Telephony.Sms.TYPE} = $INBOX AND ${Telephony.Sms.READ} = 0", null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val tid = c.getLong(0)
                    if (c.getLong(1) > store.threadSeen(tid)) set += tid
                }
            }
        }
        return set.size
    }

    fun threadIdFor(ctx: Context, address: String): Long? {
        var found: Long? = null
        runCatching {
            ctx.contentResolver.query(
                Telephony.Sms.CONTENT_URI, arrayOf(Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS),
                null, null, Telephony.Sms.DATE + " DESC",
            )?.use { c ->
                var n = 0
                while (c.moveToNext() && n++ < 6000) {
                    val a = c.getString(1) ?: continue
                    if (a == address || Phone.same(a, address)) { found = c.getLong(0); break }
                }
            }
        }
        return found
    }

    fun messages(ctx: Context, threadId: Long): List<SmsMessage> {
        val out = ArrayList<SmsMessage>()
        runCatching {
            ctx.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
                "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()), Telephony.Sms.DATE + " ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val type = c.getInt(3)
                    out += SmsMessage(c.getLong(0), c.getString(1) ?: "", c.getLong(2), type != INBOX,
                        type == Telephony.Sms.MESSAGE_TYPE_FAILED)
                }
            }
        }
        return out
    }

    /** Varsayılan SMS uygulaması olmadığımız için sistem gönderilen mesajı SMS deposuna kendisi yazar. */
    fun send(ctx: Context, address: String, body: String) {
        val sm = ctx.getSystemService(SmsManager::class.java)
        val parts = sm.divideMessage(body)
        if (parts.size > 1) sm.sendMultipartTextMessage(address, null, parts, null, null)
        else sm.sendTextMessage(address, null, body, null, null)
    }

    fun changes(ctx: Context): Flow<Unit> = callbackFlow {
        val obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        ctx.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, obs)
        ctx.contentResolver.registerContentObserver(android.net.Uri.parse("content://mms-sms/"), true, obs)
        awaitClose { ctx.contentResolver.unregisterContentObserver(obs) }
    }
}
