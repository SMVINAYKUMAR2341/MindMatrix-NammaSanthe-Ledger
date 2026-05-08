package com.nammasanthe.ledger.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStore {
    fun saveCompressed(context: Context, source: File, quality: Int = 80, maxDim: Int = 1280): String {
        val original = BitmapFactory.decodeFile(source.absolutePath) ?: return source.absolutePath
        val scale = maxDim.toFloat() / maxOf(original.width, original.height)
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            )
        } else original
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val out = File(dir, "img_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { resized.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        return out.absolutePath
    }

    fun copyToCache(context: Context, uri: Uri): File {
        val dir = File(context.cacheDir, "imports").apply { mkdirs() }
        val out = File(dir, "import_${System.currentTimeMillis()}.jpg")
        val resolver = context.contentResolver
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        } ?: error("Unable to open selected image")
        return out
    }
}
