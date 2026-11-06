package com.egepaksoy.kiss.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.widget.Toast

data class Contact(val id: Long, val name: String, val number: String)

data class CallEntry(val number: String, val name: String?, val type: Int, val date: Long) {
    val missed get() = type == CallLog.Calls.MISSED_TYPE || type == CallLog.Calls.REJECTED_TYPE
    val kind get() = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "Gelen"
        CallLog.Calls.OUTGOING_TYPE -> "Giden"
        CallLog.Calls.MISSED_TYPE -> "Cevapsız"
        CallLog.Calls.REJECTED_TYPE -> "Reddedildi"
        CallLog.Calls.BLOCKED_TYPE -> "Engellendi"
        else -> "Arama"
    }
}

object Phone {
    private val nameCache = HashMap<String, String?>()

    fun contacts(ctx: Context): List<Contact> {
        val out = ArrayList<Contact>()
        val seen = HashSet<String>()
        runCatching {
            ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " COLLATE LOCALIZED ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val name = c.getString(1) ?: continue
                    val num = c.getString(2) ?: continue
                    val key = id.toString() + ":" + digits(num).takeLast(10)
                    if (seen.add(key)) out += Contact(id, name, num)
                }
            }
        }
        return out
    }

    fun calls(ctx: Context, limit: Int = 80): List<CallEntry> {
        val out = ArrayList<CallEntry>()
        runCatching {
            ctx.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE),
                null, null, CallLog.Calls.DATE + " DESC",
            )?.use { c ->
                while (c.moveToNext() && out.size < limit) {
                    out += CallEntry(c.getString(0) ?: "", c.getString(1)?.takeIf { it.isNotBlank() }, c.getInt(2), c.getLong(3))
                }
            }
        }
        return out
    }

    fun nameFor(ctx: Context, number: String): String? {
        if (number.isBlank()) return null
        synchronized(nameCache) { if (nameCache.containsKey(number)) return nameCache[number] }
        val name = runCatching {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
        synchronized(nameCache) { nameCache[number] = name }
        return name
    }

    fun clearCache() = synchronized(nameCache) { nameCache.clear() }

    fun digits(s: String) = s.filter { it.isDigit() }

    fun same(a: String, b: String): Boolean =
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 31) PhoneNumberUtils.areSamePhoneNumber(a, b, "tr")
            else digits(a).takeLast(10) == digits(b).takeLast(10)
        }.getOrElse { digits(a).takeLast(10) == digits(b).takeLast(10) }

    fun call(ctx: Context, number: String) {
        val clean = number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (clean.isEmpty()) return
        val uri = Uri.fromParts("tel", clean, null)
        if (ctx.checkSelfPermission(android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            return
        }
        val ok = runCatching {
            ctx.getSystemService(TelecomManager::class.java).placeCall(uri, Bundle())
        }.isSuccess
        if (!ok) {
            runCatching { ctx.startActivity(Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onFailure { Toast.makeText(ctx, "Arama başlatılamadı", Toast.LENGTH_SHORT).show() }
        }
    }
}
