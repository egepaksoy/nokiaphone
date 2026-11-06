package com.egepaksoy.kiss.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size

object Media {
    private val thumbs = object : LruCache<Long, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: Bitmap) = value.byteCount
    }

    fun uri(id: Long): Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

    fun photos(ctx: Context): List<Long> {
        val out = ArrayList<Long>()
        runCatching {
            ctx.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                null, null, MediaStore.Images.Media.DATE_TAKEN + " DESC, " + MediaStore.Images.Media.DATE_ADDED + " DESC",
            )?.use { c -> while (c.moveToNext()) out += c.getLong(0) }
        }
        return out
    }

    fun cachedThumb(id: Long): Bitmap? = thumbs.get(id)

    fun thumb(ctx: Context, id: Long, px: Int = 320): Bitmap? {
        thumbs.get(id)?.let { return it }
        return runCatching { ctx.contentResolver.loadThumbnail(uri(id), Size(px, px), null) }
            .getOrNull()?.also { thumbs.put(id, it) }
    }

    fun full(ctx: Context, id: Long, maxPx: Int): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri(id))) { d, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val scale = maxOf(1, maxOf(w, h) / maxPx)
            d.setTargetSampleSize(scale)
        }
    }.getOrNull()
}
