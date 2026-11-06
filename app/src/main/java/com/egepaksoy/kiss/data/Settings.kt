package com.egepaksoy.kiss.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class KissSettings(
    val camera: Boolean = true,
    val gallery: Boolean = true,
    val torch: Boolean = true,
    val alarm: Boolean = true,
    val calculator: Boolean = true,
    val notes: Boolean = true,
    val notesPackage: String? = null,
    val allowedApps: Set<String> = emptySet(),
    val strict: Boolean = false,
    val notificationPanel: Boolean = true,
    val suspendHidden: Boolean = true,
    val waitSeconds: Int = 30,
    val pin: String? = null,
    val onboarded: Boolean = false,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("kiss", Context.MODE_PRIVATE)
    private val internal = context.getSharedPreferences("kiss_internal", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<KissSettings> = _state.asStateFlow()
    val value: KissSettings get() = _state.value

    @Synchronized
    fun update(block: (KissSettings) -> KissSettings) {
        val next = block(_state.value)
        write(next)
        _state.value = next
    }

    private fun read() = KissSettings(
        camera = prefs.getBoolean("camera", true),
        gallery = prefs.getBoolean("gallery", true),
        torch = prefs.getBoolean("torch", true),
        alarm = prefs.getBoolean("alarm", true),
        calculator = prefs.getBoolean("calculator", true),
        notes = prefs.getBoolean("notes", true),
        notesPackage = prefs.getString("notesPackage", null),
        allowedApps = prefs.getStringSet("allowedApps", emptySet())!!.toSet(),
        strict = prefs.getBoolean("strict", false),
        notificationPanel = prefs.getBoolean("notificationPanel", true),
        suspendHidden = prefs.getBoolean("suspendHidden", true),
        waitSeconds = prefs.getInt("waitSeconds", 30),
        pin = prefs.getString("pin", null),
        onboarded = prefs.getBoolean("onboarded", false),
    )

    private fun write(s: KissSettings) {
        prefs.edit()
            .putBoolean("camera", s.camera)
            .putBoolean("gallery", s.gallery)
            .putBoolean("torch", s.torch)
            .putBoolean("alarm", s.alarm)
            .putBoolean("calculator", s.calculator)
            .putBoolean("notes", s.notes)
            .putString("notesPackage", s.notesPackage)
            .putStringSet("allowedApps", s.allowedApps)
            .putBoolean("strict", s.strict)
            .putBoolean("notificationPanel", s.notificationPanel)
            .putBoolean("suspendHidden", s.suspendHidden)
            .putInt("waitSeconds", s.waitSeconds)
            .putString("pin", s.pin)
            .putBoolean("onboarded", s.onboarded)
            .commit()
    }

    // Politika ve mesajlar için iç durum (kullanıcı ayarı değil)
    var suspendedPackages: Set<String>
        get() = internal.getStringSet("suspended", emptySet())!!.toSet()
        set(v) { internal.edit().putStringSet("suspended", v).commit() }

    var temporaryPackages: Set<String>
        get() = internal.getStringSet("temporary", emptySet())!!.toSet()
        set(v) { internal.edit().putStringSet("temporary", v).commit() }

    fun threadSeen(threadId: Long): Long = internal.getLong("seen_$threadId", 0L)
    fun markThreadSeen(threadId: Long) {
        internal.edit().putLong("seen_$threadId", System.currentTimeMillis()).apply()
    }
}
