package com.egepaksoy.kiss.ui.camera

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.data.Media
import com.egepaksoy.kiss.ui.Navigator
import com.egepaksoy.kiss.ui.Screen
import com.egepaksoy.kiss.ui.common.IconBtn
import com.egepaksoy.kiss.ui.common.KIcons
import com.egepaksoy.kiss.ui.common.RequirePermissions
import com.egepaksoy.kiss.ui.common.RoundBtn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

@Composable
fun CameraScreen(nav: Navigator, galleryEnabled: Boolean) {
    val ctx = LocalContext.current
    // Kamera her temada koyu; durum çubuğu simgeleri açık renk olsun.
    DisposableEffect(Unit) {
        val window = (ctx as? Activity)?.window
        val ctl = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val before = ctl?.isAppearanceLightStatusBars
        ctl?.isAppearanceLightStatusBars = false
        KissApp.instance.torch.off()
        onDispose { if (before != null) ctl.isAppearanceLightStatusBars = before }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        RequirePermissions(listOf(Manifest.permission.CAMERA), "Fotoğraf çekmek için kamera izni gerekiyor.") {
            CameraBody(nav, galleryEnabled)
        }
    }
}

private suspend fun cameraProvider(ctx: android.content.Context): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val f = ProcessCameraProvider.getInstance(ctx)
        f.addListener({ cont.resume(f.get()) }, ContextCompat.getMainExecutor(ctx))
    }

@Composable
private fun CameraBody(nav: Navigator, galleryEnabled: Boolean) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var lens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var thumb by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    val flash = remember { Animatable(0f) }

    val previewView = remember {
        PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val capture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    capture.flashMode = flashMode

    LaunchedEffect(Unit) {
        if (galleryEnabled) thumb = withContext(Dispatchers.IO) { Media.photos(ctx).firstOrNull()?.let { Media.thumb(ctx, it, 160) } }
    }
    LaunchedEffect(lens) {
        val provider = cameraProvider(ctx)
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val selector = CameraSelector.Builder().requireLensFacing(lens).build()
        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(owner, selector, preview, capture)
        }.onFailure { Toast.makeText(ctx, "Kamera açılamadı", Toast.LENGTH_SHORT).show() }
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() } }
    }

    fun shoot() {
        if (busy) return
        busy = true
        val name = "KISS_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/KISS")
        }
        val opts = ImageCapture.OutputFileOptions.Builder(
            ctx.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values,
        ).build()
        scope.launch { flash.snapTo(0.85f); flash.animateTo(0f, tween(350)) }
        capture.takePicture(opts, ContextCompat.getMainExecutor(ctx), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                busy = false
                val uri = output.savedUri ?: return
                scope.launch {
                    thumb = withContext(Dispatchers.IO) {
                        runCatching { ctx.contentResolver.loadThumbnail(uri, android.util.Size(160, 160), null) }.getOrNull()
                    }
                }
            }
            override fun onError(exception: ImageCaptureException) {
                busy = false
                Toast.makeText(ctx, "Fotoğraf kaydedilemedi", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            IconBtn(KIcons.Close, "Kapat", { nav.pop() }, tint = Color.White)
            IconBtn(
                if (flashMode == ImageCapture.FLASH_MODE_OFF) KIcons.BoltOff else KIcons.Bolt,
                "Flaş",
                {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                    Toast.makeText(ctx, when (flashMode) {
                        ImageCapture.FLASH_MODE_AUTO -> "Flaş: otomatik"
                        ImageCapture.FLASH_MODE_ON -> "Flaş: açık"
                        else -> "Flaş: kapalı"
                    }, Toast.LENGTH_SHORT).show()
                },
                tint = if (flashMode == ImageCapture.FLASH_MODE_OFF) Color.White else Color(0xFFFFD45C),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp).clip(RoundedCornerShape(24.dp))) {
            AndroidView({ previewView }, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).border(2.dp, Color.White.copy(alpha = .8f), RoundedCornerShape(14.dp))
                    .background(Color(0xFF222222))
                    .clickable(enabled = galleryEnabled) { nav.push(Screen.Gallery) },
            ) {
                thumb?.let { Image(it.asImageBitmap(), "Son fotoğraf", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Box(
                Modifier.size(80.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape).clickable { shoot() },
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White)) }
            RoundBtn(KIcons.Flip, "Kamerayı çevir", {
                lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            }, bg = Color.White.copy(alpha = .16f), fg = Color.White, size = 52.dp)
        }
    }
}
