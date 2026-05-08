package com.nammasanthe.ledger.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraController(val imageCapture: ImageCapture)

@Composable
fun CameraPreview(
    onReady: (CameraController) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier.fillMaxSize()) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture
                )
                onReady(CameraController(capture))
            } catch (e: Throwable) {
                Log.e("CameraPreview", "binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}

suspend fun captureToFile(controller: CameraController, context: Context): File =
    suspendCancellableCoroutine { cont ->
        val outFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        val output = ImageCapture.OutputFileOptions.Builder(outFile).build()
        controller.imageCapture.takePicture(
            output,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    cont.resume(outFile)
                }
                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            }
        )
    }

fun decodeAndOrient(file: File, maxDim: Int = 1600): Bitmap {
    val opts = BitmapFactory.Options()
    val raw = BitmapFactory.decodeFile(file.absolutePath, opts) ?: error("decode failed")
    val scale = maxDim.toFloat() / maxOf(raw.width, raw.height)
    val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull()
    val orientation = exif?.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    ) ?: ExifInterface.ORIENTATION_NORMAL
    val needsScale = scale < 1f
    val needsRotate = orientation != ExifInterface.ORIENTATION_NORMAL
    if (!needsScale && !needsRotate) return raw
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.preScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.preScale(-1f, 1f)
        }
    }
    if (needsScale) matrix.postScale(scale, scale)
    return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
}
