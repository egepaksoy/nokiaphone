package com.egepaksoy.kiss.ui.sms

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.data.Contact
import com.egepaksoy.kiss.data.Phone
import com.egepaksoy.kiss.data.Sms
import com.egepaksoy.kiss.data.SmsMessage
import com.egepaksoy.kiss.data.SmsThread
import com.egepaksoy.kiss.ui.Navigator
import com.egepaksoy.kiss.ui.Screen
import com.egepaksoy.kiss.ui.common.Avatar
import com.egepaksoy.kiss.ui.common.IconBtn
import com.egepaksoy.kiss.ui.common.KIcons
import com.egepaksoy.kiss.ui.common.RequirePermissions
import com.egepaksoy.kiss.ui.common.RoundBtn
import com.egepaksoy.kiss.ui.common.TopBar
import com.egepaksoy.kiss.ui.common.shortTime
import com.egepaksoy.kiss.ui.phone.Empty
import com.egepaksoy.kiss.ui.phone.SearchField
import com.egepaksoy.kiss.ui.theme.Kiss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SMS_PERMS = listOf(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS)

@Composable
fun MessagesScreen(nav: Navigator) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar("Mesajlar", { nav.pop() }) {
            IconBtn(KIcons.Plus, "Yeni mesaj", { nav.push(Screen.NewMessage) })
        }
        RequirePermissions(SMS_PERMS, "Mesajları göstermek için SMS ve rehber izni gerekiyor.") {
            ThreadList(nav)
        }
    }
}

@Composable
private fun ThreadList(nav: Navigator) {
    val ctx = LocalContext.current
    val c = Kiss.colors
    val tick by KissApp.instance.resumeTick.collectAsState()
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { Sms.changes(ctx).collect { reload++ } }
    val threads by produceState<List<SmsThread>?>(null, tick, reload) {
        value = withContext(Dispatchers.IO) { Sms.threads(ctx) }
    }
    val list = threads ?: return
    if (list.isEmpty()) { Empty("Mesaj yok"); return }
    LazyColumn(Modifier.fillMaxSize()) {
        items(list, key = { it.threadId }) { t ->
            Row(
                Modifier.padding(horizontal = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .clickable { nav.push(Screen.Thread(t.threadId, t.address)) }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Avatar(t.title)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                        if (t.unread) Box(Modifier.padding(start = 8.dp).size(8.dp).clip(CircleShape).background(c.accent))
                    }
                    Text(
                        (if (t.fromMe) "Sen: " else "") + t.snippet.replace('\n', ' '),
                        color = if (t.unread) c.fg else c.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(shortTime(t.date), fontSize = 12.sp, color = c.muted, modifier = Modifier.align(Alignment.Top).padding(top = 3.dp))
            }
        }
    }
}

@Composable
fun NewMessageScreen(nav: Navigator) {
    val ctx = LocalContext.current
    val c = Kiss.colors
    var q by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar("Yeni mesaj", { nav.pop() })
        RequirePermissions(listOf(Manifest.permission.READ_CONTACTS), "Kişi seçmek için rehber izni gerekiyor.") {
            val contacts by produceState(emptyList<Contact>()) { value = withContext(Dispatchers.IO) { Phone.contacts(ctx) } }
            val filtered = remember(q, contacts) {
                val qq = q.trim().lowercase()
                if (qq.isEmpty()) contacts else contacts.filter { it.name.lowercase().contains(qq) || Phone.digits(it.number).contains(qq) }
            }
            Column {
                SearchField(q, "İsim ya da numara") { q = it }
                val digits = Phone.digits(q)
                LazyColumn(Modifier.fillMaxSize()) {
                    if (digits.length >= 3 && q.trim().all { it.isDigit() || it == '+' || it == ' ' }) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().clickable { nav.replace(Screen.Thread(null, q.trim())) }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                            ) { Text("Şu numaraya yaz: ${q.trim()}", color = c.accent, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                    items(filtered, key = { "${it.id}:${it.number}" }) { ct ->
                        Row(
                            Modifier.padding(horizontal = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .clickable { nav.replace(Screen.Thread(null, ct.number)) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Avatar(ct.name)
                            Column {
                                Text(ct.name, fontWeight = FontWeight.SemiBold)
                                Text(ct.number, color = c.muted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThreadScreen(threadIdArg: Long?, address: String, nav: Navigator) {
    val ctx = LocalContext.current
    val name by produceState<String?>(null, address) { value = withContext(Dispatchers.IO) { Phone.nameFor(ctx, address) } }
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        TopBar(name ?: address, { nav.pop() }) {
            IconBtn(KIcons.Phone, "Ara", { Phone.call(ctx, address) })
        }
        RequirePermissions(SMS_PERMS + Manifest.permission.SEND_SMS, "Mesajlaşmak için SMS izinleri gerekiyor.") {
            ThreadBody(threadIdArg, address)
        }
    }
}

@Composable
private fun ThreadBody(threadIdArg: Long?, address: String) {
    val ctx = LocalContext.current
    val c = Kiss.colors
    val scope = rememberCoroutineScope()
    val store = KissApp.instance.settings
    var threadId by remember { mutableStateOf(threadIdArg) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { Sms.changes(ctx).collect { reload++ } }
    val messages by produceState(emptyList<SmsMessage>(), threadId, reload) {
        val tid = threadId ?: withContext(Dispatchers.IO) { Sms.threadIdFor(ctx, address) }?.also { threadId = it }
        if (tid != null) {
            value = withContext(Dispatchers.IO) { Sms.messages(ctx, tid) }
            store.markThreadSeen(tid)
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1) }

    var text by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
        ) {
            items(messages, key = { it.id }) { m -> Bubble(m) }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(c.soft)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (text.isEmpty()) Text("Mesaj yaz", color = c.muted)
                BasicTextField(
                    text, { text = it },
                    textStyle = TextStyle(color = c.fg, fontSize = 16.sp),
                    cursorBrush = SolidColor(c.accent), maxLines = 5,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            RoundBtn(KIcons.Send, "Gönder", {
                val body = text.trim()
                if (body.isEmpty()) return@RoundBtn
                runCatching { Sms.send(ctx, address, body) }
                    .onSuccess {
                        text = ""
                        scope.launch { delay(800); reload++ }
                    }
                    .onFailure { Toast.makeText(ctx, "Mesaj gönderilemedi", Toast.LENGTH_SHORT).show() }
            }, bg = if (text.isBlank()) c.soft else c.accent, fg = if (text.isBlank()) c.muted else c.onAccent)
        }
    }
}

@Composable
private fun Bubble(m: SmsMessage) {
    val c = Kiss.colors
    val time = Instant.ofEpochMilli(m.date).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM HH:mm"))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromMe) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 290.dp).clip(
                RoundedCornerShape(20.dp, 20.dp, if (m.fromMe) 6.dp else 20.dp, if (m.fromMe) 20.dp else 6.dp),
            ).background(if (m.fromMe) c.me else c.soft).padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text(m.body, color = if (m.fromMe) c.onMe else c.fg, fontSize = 16.sp)
            Text(
                if (m.failed) "Gönderilemedi" else time,
                color = if (m.fromMe) c.onMe.copy(alpha = 0.7f) else c.muted, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}
