package com.egepaksoy.kiss.ui.phone

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.data.CallEntry
import com.egepaksoy.kiss.data.Contact
import com.egepaksoy.kiss.data.Phone
import com.egepaksoy.kiss.ui.Navigator
import com.egepaksoy.kiss.ui.Screen
import com.egepaksoy.kiss.data.KissSettings
import com.egepaksoy.kiss.data.Sms
import com.egepaksoy.kiss.ui.common.IconBtn
import com.egepaksoy.kiss.ui.sms.ThreadList
import com.egepaksoy.kiss.ui.common.Avatar
import com.egepaksoy.kiss.ui.common.KIcons
import com.egepaksoy.kiss.ui.common.Keypad
import com.egepaksoy.kiss.ui.common.RequirePermissions
import com.egepaksoy.kiss.ui.common.RoundBtn
import com.egepaksoy.kiss.ui.common.TopBar
import com.egepaksoy.kiss.ui.common.shortTime
import com.egepaksoy.kiss.ui.theme.Display
import com.egepaksoy.kiss.ui.theme.Kiss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class PhoneTab(val label: String) { Keys("Tuşlar"), Recent("Son"), Messages("Mesajlar"), Contacts("Kişiler") }

/** Arama ve SMS tek sayfada. */
@Composable
fun PhoneScreen(initialTab: Int, s: KissSettings, nav: Navigator) {
    val ctx = LocalContext.current
    val tick by KissApp.instance.resumeTick.collectAsState()
    val unread by produceState(0, tick) {
        value = withContext(Dispatchers.IO) { Sms.unreadCount(ctx) }
        Sms.changes(ctx).collect { value = withContext(Dispatchers.IO) { Sms.unreadCount(ctx) } }
    }
    var tab by rememberSaveable(initialTab) {
        mutableStateOf(
            PhoneTab.entries.getOrNull(initialTab)
                ?: if (Sms.unreadCount(ctx) > 0) PhoneTab.Messages else PhoneTab.Keys
        )
    }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar("Telefon", { nav.pop() }) {
            if (tab == PhoneTab.Messages) IconBtn(KIcons.Plus, "Yeni mesaj", { nav.push(Screen.NewMessage) })
        }
        Segmented(PhoneTab.entries, tab, { it.label }, dot = { it == PhoneTab.Messages && unread > 0 }) { tab = it }
        Box(Modifier.weight(1f)) {
            when (tab) {
                PhoneTab.Keys -> KeysTab()
                PhoneTab.Messages -> RequirePermissions(
                    listOf(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS),
                    "Mesajları göstermek için SMS ve rehber izni gerekiyor.",
                ) { ThreadList(nav, s.messagingApps) }
                PhoneTab.Recent -> RequirePermissions(
                    listOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS),
                    "Son aramaları göstermek için arama kaydı izni gerekiyor.",
                ) { RecentTab(nav) }
                PhoneTab.Contacts -> RequirePermissions(
                    listOf(Manifest.permission.READ_CONTACTS),
                    "Kişileri göstermek için rehber izni gerekiyor.",
                ) { ContactsTab(nav) }
            }
        }
    }
}

@Composable
fun <T> Segmented(items: List<T>, selected: T, label: (T) -> String, dot: (T) -> Boolean = { false }, onSelect: (T) -> Unit) {
    val c = Kiss.colors
    Row(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.soft).padding(3.dp),
    ) {
        items.forEach { t ->
            val on = t == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if (on) c.bg else c.soft)
                    .clickable { onSelect(t) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label(t), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.fg else c.muted)
                    if (dot(t)) Box(Modifier.padding(start = 5.dp).size(7.dp).clip(CircleShape).background(c.accent))
                }
            }
        }
    }
}

private fun formatNumber(d: String): String {
    if (d.any { it == '*' || it == '#' } || d.startsWith("+")) return d
    if (d.length <= 4) return d
    val parts = listOf(d.take(4), d.drop(4).take(3), d.drop(7).take(2), d.drop(9))
    return parts.filter { it.isNotEmpty() }.joinToString(" ")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeysTab() {
    val ctx = LocalContext.current
    val c = Kiss.colors
    var digits by rememberSaveable { mutableStateOf("") }
    val contacts by produceState(emptyList<Contact>()) {
        if (ctx.checkSelfPermission(Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED)
            value = withContext(Dispatchers.IO) { Phone.contacts(ctx) }
    }
    val match = remember(digits, contacts) {
        if (digits.length < 3) null
        else contacts.firstOrNull { Phone.digits(it.number).contains(digits) }
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Text(
            if (digits.isEmpty()) "Numara gir" else formatNumber(digits),
            fontFamily = if (digits.isEmpty()) null else Display,
            fontSize = if (digits.isEmpty()) 17.sp else 34.sp,
            color = if (digits.isEmpty()) c.muted else c.fg,
            textAlign = TextAlign.Center, maxLines = 2,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            match?.let { "${it.name} · ${it.number}" } ?: " ",
            color = c.accent, fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp).clickable(enabled = match != null) {
                match?.let { digits = Phone.digits(it.number) }
            },
        )
        Spacer(Modifier.weight(1f))
        Keypad(
            onKey = { if (digits.length < 20) digits += it },
            onLongKey = { if (it == '0') digits += "+" else if (digits.length < 20) digits += it },
        )
        Row(
            Modifier.padding(top = 16.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(72.dp))
            RoundBtn(KIcons.Phone, "Ara", { Phone.call(ctx, digits) }, bg = c.call, fg = androidx.compose.ui.graphics.Color.White, size = 72.dp, iconSize = 28.dp)
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                if (digits.isNotEmpty()) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).combinedClickable(
                            onClick = { digits = digits.dropLast(1) },
                            onLongClick = { digits = "" },
                        ),
                        contentAlignment = Alignment.Center,
                    ) { Icon(KIcons.Backspace, "Sil", tint = c.muted) }
                }
            }
        }
    }
}

@Composable
private fun RecentTab(nav: Navigator) {
    val ctx = LocalContext.current
    val c = Kiss.colors
    val tick by KissApp.instance.resumeTick.collectAsState()
    val calls by produceState<List<CallEntry>?>(null, tick) { value = withContext(Dispatchers.IO) { Phone.calls(ctx) } }
    var open by remember { mutableStateOf<Int?>(null) }
    val list = calls ?: return
    if (list.isEmpty()) { Empty("Henüz arama yok"); return }
    LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
        items(list.size) { i ->
            val e = list[i]
            val title = e.name ?: e.number.ifBlank { "Gizli numara" }
            PersonRow(
                title = title,
                subtitle = e.kind,
                trailing = shortTime(e.date),
                danger = e.missed,
                expanded = open == i,
                onClick = { open = if (open == i) null else i },
                onCall = { Phone.call(ctx, e.number) },
                onMessage = if (e.number.isNotBlank()) ({ nav.push(Screen.Thread(null, e.number)) }) else null,
            )
        }
    }
}

@Composable
private fun ContactsTab(nav: Navigator) {
    val ctx = LocalContext.current
    val c = Kiss.colors
    val contacts by produceState<List<Contact>?>(null) { value = withContext(Dispatchers.IO) { Phone.contacts(ctx) } }
    var q by rememberSaveable { mutableStateOf("") }
    var open by remember { mutableStateOf<String?>(null) }
    val list = contacts ?: return
    val filtered = remember(q, list) {
        val qq = q.trim().lowercase()
        if (qq.isEmpty()) list else list.filter { it.name.lowercase().contains(qq) || Phone.digits(it.number).contains(qq) }
    }
    Column(Modifier.fillMaxSize()) {
        SearchField(q, "Kişi ara") { q = it }
        if (filtered.isEmpty()) { Empty(if (list.isEmpty()) "Rehberde kişi yok" else "Sonuç yok"); return@Column }
        LazyColumn(Modifier.fillMaxSize()) {
            var last: Char? = null
            filtered.forEach { ct ->
                val l = ct.name.first().uppercaseChar()
                if (l != last && q.isBlank()) {
                    last = l
                    item(key = "h$l${ct.id}") {
                        Text(l.toString(), color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 2.dp))
                    }
                }
                val key = "${ct.id}:${ct.number}"
                item(key = key) {
                    PersonRow(
                        title = ct.name, subtitle = ct.number, expanded = open == key,
                        onClick = { open = if (open == key) null else key },
                        onCall = { Phone.call(ctx, ct.number) },
                        onMessage = { nav.push(Screen.Thread(null, ct.number)) },
                    )
                }
            }
        }
    }
}

@Composable
fun SearchField(value: String, hint: String, onChange: (String) -> Unit) {
    val c = Kiss.colors
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(c.soft).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(KIcons.Search, null, tint = c.muted, modifier = Modifier.size(18.dp))
        Box(Modifier.padding(start = 10.dp).weight(1f)) {
            if (value.isEmpty()) Text(hint, color = c.muted, fontSize = 16.sp)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = TextStyle(color = c.fg, fontSize = 16.sp),
                cursorBrush = SolidColor(c.accent), modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun PersonRow(
    title: String,
    subtitle: String,
    trailing: String? = null,
    danger: Boolean = false,
    expanded: Boolean,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onMessage: (() -> Unit)?,
) {
    val c = Kiss.colors
    Column(Modifier.padding(horizontal = 8.dp).clip(RoundedCornerShape(16.dp)).background(if (expanded) c.soft else c.bg)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(title)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = if (danger) c.end else c.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 13.sp, color = if (danger) c.end else c.muted, maxLines = 1)
            }
            if (trailing != null) Text(trailing, fontSize = 12.sp, color = c.muted)
        }
        AnimatedVisibility(expanded) {
            Row(Modifier.padding(start = 68.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionChip("Ara", KIcons.Phone, c.call, onCall)
                if (onMessage != null) ActionChip("Mesaj", KIcons.Message, c.accent, onMessage)
            }
        }
    }
}

@Composable
private fun ActionChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val c = Kiss.colors
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(c.bg).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun Empty(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Kiss.colors.muted)
    }
}

