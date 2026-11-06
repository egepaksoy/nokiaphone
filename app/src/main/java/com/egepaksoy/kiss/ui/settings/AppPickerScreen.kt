package com.egepaksoy.kiss.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.data.AppEntry
import com.egepaksoy.kiss.data.Apps
import com.egepaksoy.kiss.data.KissSettings
import com.egepaksoy.kiss.ui.Navigator
import com.egepaksoy.kiss.ui.common.TopBar
import com.egepaksoy.kiss.ui.phone.SearchField
import com.egepaksoy.kiss.ui.theme.Kiss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppPickerScreen(s: KissSettings, notes: Boolean, nav: Navigator) {
    val ctx = LocalContext.current
    val store = KissApp.instance.settings
    var q by rememberSaveable { mutableStateOf("") }
    val all by produceState<List<AppEntry>?>(null) { value = withContext(Dispatchers.IO) { Apps.launchable(ctx) } }
    val filtered = remember(q, all) {
        val list = all ?: emptyList()
        if (q.isBlank()) list else list.filter { it.label.contains(q.trim(), ignoreCase = true) }
    }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar(if (notes) "Not uygulaması" else "İzin verilen uygulamalar", { nav.pop() })
        Text(
            if (notes) "Ana ekrandaki \"Notlar\" bu uygulamayı açar."
            else "Açtığın uygulamalar ana ekranda görünür ve sıkı modda kullanılabilir.",
            color = Kiss.colors.muted, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp),
        )
        SearchField(q, "Uygulama ara") { q = it }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
            items(filtered, key = { it.pkg }) { a ->
                if (notes) {
                    AppRow(a, s.notesPackage == a.pkg, single = true) {
                        store.update { it.copy(notesPackage = a.pkg, notes = true) }
                        nav.pop()
                    }
                } else {
                    AppRow(a, a.pkg in s.allowedApps) { v ->
                        store.update { it.copy(allowedApps = if (v) it.allowedApps + a.pkg else it.allowedApps - a.pkg) }
                    }
                }
            }
        }
    }
}
