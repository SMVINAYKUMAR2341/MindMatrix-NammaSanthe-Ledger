package com.nammasanthe.ledger.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages photo proof capture and storage for transactions.
 * Photos are stored in app's private storage for security.
 */
object PhotoProofManager {

    private const val PHOTO_DIR = "transaction_photos"
    private const val MAX_PHOTO_WIDTH = 1280
    private const val COMPRESS_QUALITY = 85

    /**
     * Create a temporary file URI for camera capture.
     */
    fun createTempPhotoUri(context: Context): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val tempFile = File(context.cacheDir, "TEMP_${timeStamp}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    /**
     * Save a photo permanently for a transaction.
     * Compresses and resizes for efficient storage.
     */
    suspend fun saveTransactionPhoto(
        context: Context,
        sourceUri: Uri,
        transactionId: Long
    ): String? = withContext(Dispatchers.IO) {
        try {
            val photoDir = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
            val photoFile = File(photoDir, "txn_${transactionId}_${System.currentTimeMillis()}.jpg")

            // Load and compress bitmap
            val bitmap = loadAndResizeBitmap(context, sourceUri)
                ?: return@withContext null

            // Save compressed JPEG
            FileOutputStream(photoFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, out)
            }

            // Clean up original if it was a temp file
            if (sourceUri.path?.contains("cache") == true) {
                File(sourceUri.path!!).delete()
            }

            photoFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Load a transaction photo by path.
     */
    fun loadPhoto(photoPath: String): Bitmap? {
        return if (File(photoPath).exists()) {
            BitmapFactory.decodeFile(photoPath)
        } else null
    }

    /**
     * Delete a transaction photo.
     */
    fun deletePhoto(photoPath: String?): Boolean {
        return photoPath?.let { path ->
            File(path).delete()
        } ?: false
    }

    /**
     * Check if photo exists.
     */
    fun photoExists(photoPath: String?): Boolean {
        return photoPath?.let { File(it).exists() } ?: false
    }

    /**
     * Clean up orphaned photos (not associated with any transaction).
     */
    suspend fun cleanupOrphanedPhotos(
        context: Context,
        validPhotoPaths: List<String>
    ) = withContext(Dispatchers.IO) {
        val photoDir = File(context.filesDir, PHOTO_DIR)
        if (!photoDir.exists()) return@withContext

        photoDir.listFiles()?.forEach { file ->
            if (file.absolutePath !in validPhotoPaths) {
                file.delete()
            }
        }
    }

    private fun loadAndResizeBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            // First decode bounds to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            // Calculate sample size
            val sampleSize = calculateInSampleSize(options, MAX_PHOTO_WIDTH, MAX_PHOTO_WIDTH)

            // Decode actual bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
