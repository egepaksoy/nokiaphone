package com.egepaksoy.kiss.ui.gallery

import android.Manifest
import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.data.Media
import com.egepaksoy.kiss.ui.Navigator
import com.egepaksoy.kiss.ui.Screen
import com.egepaksoy.kiss.ui.common.IconBtn
import com.egepaksoy.kiss.ui.common.KIcons
import com.egepaksoy.kiss.ui.common.RequirePermissions
import com.egepaksoy.kiss.ui.common.TopBar
import com.egepaksoy.kiss.ui.phone.Empty
import com.egepaksoy.kiss.ui.theme.Kiss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val MEDIA_PERM = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
else Manifest.permission.READ_EXTERNAL_STORAGE

/** Galeri ekranları arasında paylaşılan liste; görüntüleyici aynı sırayı kullanır. */
private object GalleryState { var ids: List<Long> = emptyList() }

@Composable
fun GalleryScreen(nav: Navigator) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar("Galeri", { nav.pop() })
        RequirePermissions(listOf(MEDIA_PERM), "Fotoğrafları göstermek için galeri izni gerekiyor.") {
            val ctx = LocalContext.current
            val tick by KissApp.instance.resumeTick.collectAsState()
            val ids by produceState<List<Long>?>(null, tick) {
                value = withContext(Dispatchers.IO) { Media.photos(ctx) }.also { GalleryState.ids = it }
            }
            val list = ids ?: return@RequirePermissions
            if (list.isEmpty()) { Empty("Henüz fotoğraf yok"); return@RequirePermissions }
            Text("${list.size} fotoğraf", color = Kiss.colors.muted, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, bottom = 8.dp))
            LazyVerticalGrid(
                GridCells.Fixed(3), Modifier.fillMaxSize().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(list.size, key = { list[it] }) { i ->
                    Thumb(list[i]) { nav.push(Screen.Photo(i)) }
                }
            }
        }
    }
}

@Composable
private fun Thumb(id: Long, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(Media.cachedThumb(id), id) {
        if (value == null) value = withContext(Dispatchers.IO) { Media.thumb(ctx, id) }
    }
    Box(Modifier.aspectRatio(1f).background(Kiss.colors.soft).clickable(onClick = onClick)) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoScreen(start: Int, nav: Navigator) {
    val ctx = LocalContext.current
    var ids by remember { androidx.compose.runtime.mutableStateOf(GalleryState.ids) }
    if (ids.isEmpty()) { nav.pop(); return }
    val pager = rememberPagerState(initialPage = start.coerceIn(0, ids.lastIndex)) { ids.size }
    val maxPx = LocalConfiguration.current.screenHeightDp * 3

    DisposableEffect(Unit) {
        val window = (ctx as? Activity)?.window
        val ctl = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val before = ctl?.isAppearanceLightStatusBars
        ctl?.isAppearanceLightStatusBars = false
        onDispose { if (before != null) ctl.isAppearanceLightStatusBars = before }
    }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val removed = ids.getOrNull(pager.currentPage)
            ids = ids.filter { it != removed }
            GalleryState.ids = ids
            if (ids.isEmpty()) nav.pop()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (ids.isNotEmpty()) {
            HorizontalPager(pager, Modifier.fillMaxSize(), key = { ids.getOrElse(it) { -1L } }) { page ->
                val id = ids.getOrNull(page) ?: return@HorizontalPager
                val bmp by produceState<Bitmap?>(Media.cachedThumb(id), id) {
                    value = withContext(Dispatchers.IO) { Media.full(ctx, id, maxPx) } ?: value
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit) }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().systemBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconBtn(KIcons.Back, "Geri", { nav.pop() }, tint = Color.White)
            IconBtn(KIcons.Trash, "Sil", {
                val id = ids.getOrNull(pager.currentPage) ?: return@IconBtn
                runCatching {
                    if (Build.VERSION.SDK_INT >= 30) {
                        val pi = MediaStore.createDeleteRequest(ctx.contentResolver, listOf(Media.uri(id)))
                        deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                    } else if (ctx.contentResolver.delete(Media.uri(id), null, null) > 0) {
                        ids = ids.filter { it != id }
                        GalleryState.ids = ids
                        if (ids.isEmpty()) nav.pop()
                    }
                }
            }, tint = Color.White)
        }
    }
}
