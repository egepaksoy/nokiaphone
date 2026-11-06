package com.egepaksoy.kiss.ui.common

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.ui.theme.Display
import com.egepaksoy.kiss.ui.theme.Kiss
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBtn(KIcons.Back, "Geri", onBack)
        Text(
            title, Modifier.weight(1f).padding(start = 4.dp),
            fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, maxLines = 1,
        )
        actions()
    }
}

@Composable
fun IconBtn(icon: ImageVector, desc: String, onClick: () -> Unit, tint: Color = Kiss.colors.fg, size: Dp = 44.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(22.dp)) }
}

@Composable
fun RoundBtn(
    icon: ImageVector, desc: String, onClick: () -> Unit,
    bg: Color = Kiss.colors.soft, fg: Color = Kiss.colors.fg, size: Dp = 48.dp, iconSize: Dp = 22.dp,
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = fg, modifier = Modifier.size(iconSize)) }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(Locale.forLanguageTag("tr")),
        Modifier.padding(start = 8.dp, top = 18.dp, bottom = 6.dp),
        fontSize = 11.sp, letterSpacing = 1.2.sp, color = Kiss.colors.muted, fontWeight = FontWeight.Medium,
    )
}

@Composable
fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kiss.colors.soft), content = content)
}

@Composable
fun GroupRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    titleColor: Color = Kiss.colors.fg,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, color = titleColor, fontSize = 16.sp)
            if (subtitle != null) Text(subtitle, color = Kiss.colors.muted, fontSize = 13.sp, lineHeight = 17.sp)
        }
        trailing?.invoke()
    }
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(Kiss.colors.line))
}

@Composable
fun KSwitch(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val c = Kiss.colors
    val track by animateColorAsState(if (checked) c.call else c.line, label = "track")
    val x by animateDpAsState(if (checked) 19.dp else 3.dp, label = "thumb")
    Box(
        Modifier.width(46.dp).height(28.dp).clip(CircleShape).background(if (enabled) track else c.line)
            .clickable(enabled = enabled) { onChange(!checked) },
    ) {
        Box(Modifier.offset(x = x, y = 3.dp).size(22.dp).clip(CircleShape).background(if (c.dark) Color(0xFFEDEFEA) else Color.White))
    }
}

@Composable
fun Chevron() = Icon(KIcons.Chevron, null, tint = Kiss.colors.muted, modifier = Modifier.size(18.dp))

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = Kiss.colors.fg) {
    val c = Kiss.colors
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(if (enabled) color else c.line)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (enabled) c.bg else c.muted, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Keypad(
    onKey: (Char) -> Unit,
    onLongKey: ((Char) -> Unit)? = null,
    letters: Boolean = true,
    keySize: Dp = 72.dp,
    gapH: Dp = 24.dp,
    gapV: Dp = 14.dp,
    digitSize: TextUnit = 28.sp,
) {
    val keys = listOf(
        '1' to "", '2' to "ABC", '3' to "DEF", '4' to "GHI", '5' to "JKL", '6' to "MNO",
        '7' to "PQRS", '8' to "TUV", '9' to "WXYZ", '*' to "", '0' to "+", '#' to "",
    )
    val c = Kiss.colors
    Column(verticalArrangement = Arrangement.spacedBy(gapV), horizontalAlignment = Alignment.CenterHorizontally) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gapH)) {
                row.forEach { (k, l) ->
                    val visible = letters || k.isDigit()
                    Box(
                        Modifier.size(keySize).clip(CircleShape)
                            .then(if (visible) Modifier.background(c.soft).combinedClickable(
                                onClick = { onKey(k) },
                                onLongClick = onLongKey?.let { { it(k) } },
                            ) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (visible) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(k.toString(), fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = digitSize, lineHeight = digitSize)
                            if (letters && l.isNotEmpty()) Text(l, fontSize = 9.sp, letterSpacing = 1.4.sp, color = c.muted, fontWeight = FontWeight.SemiBold, lineHeight = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Avatar(name: String, size: Dp = 44.dp) {
    val c = Kiss.colors
    val ch = name.trim().firstOrNull()?.let { if (it.isLetter()) it.uppercaseChar().toString() else "#" } ?: "#"
    Box(Modifier.size(size).clip(CircleShape).background(c.soft), contentAlignment = Alignment.Center) {
        Text(ch, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun Centered(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** Gerekli izinler yoksa açıklama ve "İzin ver" düğmesi gösterir. */
@Composable
fun RequirePermissions(perms: List<String>, reason: String, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val tick by KissApp.instance.resumeTick.collectAsState()
    var bump by remember { mutableIntStateOf(0) }
    val granted = remember(tick, bump) {
        perms.all { ctx.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { bump++ }
    if (granted) content() else Centered {
        Text(reason, textAlign = TextAlign.Center, color = Kiss.colors.muted)
        Spacer(Modifier.height(20.dp))
        PrimaryButton("İzin ver", { launcher.launch(perms.toTypedArray()) })
    }
}

@Composable
fun Ticker(seconds: Int, key: Any? = Unit, onTick: (Int) -> Unit) {
    LaunchedEffect(key) {
        var left = seconds
        onTick(left)
        while (left > 0) {
            kotlinx.coroutines.delay(1000)
            left--
            onTick(left)
        }
    }
}

fun shortTime(ms: Long): String {
    val dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val d = dt.toLocalDate()
    val today = LocalDate.now()
    return when {
        d == today -> dt.format(DateTimeFormatter.ofPattern("HH:mm"))
        d == today.minusDays(1) -> "Dün"
        d.isAfter(today.minusDays(7)) -> dt.format(DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("tr")))
        else -> dt.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("tr")))
    }
}
