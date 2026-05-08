package com.nammasanthe.ledger.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nammasanthe.ledger.security.DeviceIdHelper
import com.nammasanthe.ledger.security.QrScannerAnalyzer
import com.nammasanthe.ledger.security.TrustLevel
import com.nammasanthe.ledger.ui.components.TrustBadge
import com.nammasanthe.ledger.viewmodel.ConfirmationViewModel
import com.nammasanthe.ledger.viewmodel.ScanState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerConfirmScreen(
    confirmationVm : ConfirmationViewModel,
    onBack         : () -> Unit,
    onDone         : () -> Unit
) {
    val ctx           = LocalContext.current
    val scanState     by confirmationVm.scanState.collectAsStateWithLifecycle()
    val scannerDeviceId = remember { DeviceIdHelper.getDeviceId(ctx) }

    // Camera permission
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) permLauncher.launch(Manifest.permission.CAMERA) }

    DisposableEffect(Unit) { onDispose { confirmationVm.clearScanState() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan & Confirm Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier            = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val s = scanState) {

                // ── Idle / scanning ──────────────────────────────────────────
                ScanState.Idle -> {
                    if (!hasCamera) {
                        Text("Camera permission is required to scan QR codes.")
                        Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant Camera Access")
                        }
                    } else {
                        Text(
                            "Point the camera at the vendor's QR code",
                            textAlign = TextAlign.Center,
                            color     = Color.Gray,
                            fontSize  = 13.sp
                        )
                        QrCameraView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            onDecoded = { raw ->
                                confirmationVm.onQrScanned(raw, scannerDeviceId)
                            }
                        )
                        Text(
                            "QR codes expire after 60 seconds — scan promptly.",
                            textAlign = TextAlign.Center,
                            color     = Color.Gray,
                            fontSize  = 11.sp
                        )
                    }
                }

                // ── Decoded — show details, await user decision ──────────────
                is ScanState.Decoded -> {
                    Text("Transaction Scanned", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    TrustBadge(s.trustLevel)

                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DetailRow("Transaction ID", s.payload.txnId)
                            DetailRow("Type",   s.payload.type)
                            DetailRow("Amount", "₹${"%.2f".format(s.payload.amount)}")
                        }
                    }

                    // Trust level explanation
                    if (s.trustLevel == TrustLevel.SUSPICIOUS) {
                        Card(
                            colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "⚠ Suspicious scan detected.\n" +
                                "This QR was scanned on the same device that generated it, " +
                                "or was scanned too quickly. The confirmation will be flagged.",
                                modifier = Modifier.padding(12.dp),
                                fontSize = 12.sp,
                                color    = Color(0xFF9F1239)
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick  = { confirmationVm.rejectScan() },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) { Text("Reject") }

                        Button(
                            onClick  = {
                                confirmationVm.confirmScan(s.payload, s.trustLevel, scannerDeviceId)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Confirm") }
                    }
                }

                // ── Confirmed ────────────────────────────────────────────────
                is ScanState.Confirmed -> {
                    Spacer(Modifier.weight(1f))
                    val (icon, msg, color) = when (s.trustLevel) {
                        TrustLevel.VERIFIED   -> Triple("✅", "Transaction Confirmed!",  Color(0xFF22C55E))
                        TrustLevel.SUSPICIOUS -> Triple("⚠️", "Confirmed with Warning",  Color(0xFFF59E0B))
                        TrustLevel.UNVERIFIED -> Triple("ℹ️", "Stored as Unverified",    Color(0xFF64748B))
                    }
                    Text(icon, fontSize = 52.sp)
                    Text(msg, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
                    TrustBadge(s.trustLevel)
                    Text(
                        "The confirmation has been stored securely on-device.",
                        textAlign = TextAlign.Center,
                        color     = Color.Gray,
                        fontSize  = 13.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Text("Done")
                    }
                }

                // ── Failed / rejected ────────────────────────────────────────
                is ScanState.Failed -> {
                    Spacer(Modifier.weight(1f))
                    Text("❌", fontSize = 52.sp)
                    Text(
                        s.reason,
                        fontWeight = FontWeight.SemiBold,
                        textAlign  = TextAlign.Center,
                        color      = Color(0xFFEF4444)
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick  = { confirmationVm.clearScanState() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Try Again") }
                }
            }
        }
    }
}

// ── Private camera preview with ImageAnalysis ─────────────────────────────────

@Composable
private fun QrCameraView(
    onDecoded : (String) -> Unit,
    modifier  : Modifier = Modifier
) {
    val ctx            = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val analyzer = remember { QrScannerAnalyzer(onDecoded) }

    val previewView = remember {
        PreviewView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Re-create analyzer (reset fired flag) whenever onDecoded lambda changes
    LaunchedEffect(onDecoded) { analyzer.reset() }

    AndroidView(factory = { previewView }, modifier = modifier) {
        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            val provider = future.get()
            val preview  = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { ia ->
                    ia.setAnalyzer(ContextCompat.getMainExecutor(ctx), analyzer)
                }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Throwable) {
                Log.e("QrCameraView", "CameraX binding failed", e)
            }
        }, ContextCompat.getMainExecutor(ctx))
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
