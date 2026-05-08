package com.nammasanthe.ledger.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nammasanthe.ledger.camera.CameraController
import com.nammasanthe.ledger.camera.CameraPreview
import com.nammasanthe.ledger.camera.captureToFile
import com.nammasanthe.ledger.util.ImageStore
import com.nammasanthe.ledger.viewmodel.OcrViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    ocrViewModel: OcrViewModel,
    onBack: () -> Unit,
    onResult: () -> Unit
) {
    val ctx = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCamera = it }

    val scope = rememberCoroutineScope()

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val file = withContext(Dispatchers.IO) { ImageStore.copyToCache(ctx, uri) }
            ocrViewModel.processCapturedFile(file)
            onResult()
        }
    }

    LaunchedEffect(Unit) { if (!hasCamera) launcher.launch(Manifest.permission.CAMERA) }

    var controller by remember { mutableStateOf<CameraController?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Bill") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            if (!hasCamera) {
                Text("Camera permission required for live scan.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Grant") }
                Spacer(Modifier.height(12.dp))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                if (hasCamera) {
                    CameraPreview(onReady = { controller = it })
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Camera preview unavailable")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val cam = controller ?: return@Button
                    scope.launch {
                        val file = captureToFile(cam, ctx)
                        ocrViewModel.processCapturedFile(file)
                        onResult()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasCamera
            ) {
                Icon(Icons.Default.Camera, null); Spacer(Modifier.width(8.dp))
                Text("Capture & Recognize")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { pickImage.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(8.dp))
                Text("Pick From Gallery")
            }
        }
    }
}
