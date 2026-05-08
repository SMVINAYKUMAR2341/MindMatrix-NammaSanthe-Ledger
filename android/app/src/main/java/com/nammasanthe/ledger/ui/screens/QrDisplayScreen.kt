package com.nammasanthe.ledger.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nammasanthe.ledger.security.TrustLevel
import com.nammasanthe.ledger.ui.components.TrustBadge
import com.nammasanthe.ledger.viewmodel.ConfirmationViewModel
import com.nammasanthe.ledger.viewmodel.QrDisplayState
import kotlinx.coroutines.delay

private const val QR_TTL_SECONDS = 60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrDisplayScreen(
    txnId             : Long,
    confirmationVm    : ConfirmationViewModel,
    onBack            : () -> Unit
) {
    val qrState by confirmationVm.qrState.collectAsStateWithLifecycle()

    // Request QR generation on first composition
    LaunchedEffect(txnId) {
        confirmationVm.requestQrForTxnId(txnId)
    }

    // Countdown timer — only runs while QR is showing
    var secondsLeft by remember { mutableIntStateOf(QR_TTL_SECONDS) }
    LaunchedEffect(qrState) {
        if (qrState is QrDisplayState.Ready) {
            secondsLeft = QR_TTL_SECONDS
            while (secondsLeft > 0) {
                delay(1_000)
                secondsLeft--
            }
            confirmationVm.markQrExpired()
        }
    }

    val timerFraction by animateFloatAsState(
        targetValue  = secondsLeft / QR_TTL_SECONDS.toFloat(),
        animationSpec = tween(durationMillis = 800),
        label        = "timerFraction"
    )

    DisposableEffect(Unit) { onDispose { confirmationVm.clearQr() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Confirmation QR") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier            = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (val s = qrState) {
                QrDisplayState.Idle -> {
                    Spacer(Modifier.weight(1f))
                    CircularProgressIndicator()
                    Text("Generating QR…", color = Color.Gray)
                    Spacer(Modifier.weight(1f))
                }

                QrDisplayState.Expired -> {
                    Spacer(Modifier.weight(1f))
                    Text("⏱", fontSize = 52.sp)
                    Text(
                        "QR Expired",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "The 60-second window has closed.\nGenerate a fresh QR to confirm.",
                        textAlign = TextAlign.Center,
                        color     = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick  = { confirmationVm.requestQrForTxnId(txnId) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Regenerate QR") }
                    Spacer(Modifier.weight(1f))
                }

                // ── Firebase confirmed! ──────────────────────────────────────
                is QrDisplayState.ConfirmedViaFirebase -> {
                    Spacer(Modifier.weight(1f))

                    val (icon, msg, color) = when (s.trustLevel) {
                        TrustLevel.VERIFIED   -> Triple("✅", "Customer Confirmed!",  Color(0xFF22C55E))
                        TrustLevel.SUSPICIOUS -> Triple("⚠️", "Confirmed with Warning",  Color(0xFFF59E0B))
                        TrustLevel.UNVERIFIED -> Triple("ℹ️", "Stored as Unverified",    Color(0xFF64748B))
                    }
                    Text(icon, fontSize = 64.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(msg, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
                    TrustBadge(s.trustLevel)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F1F0F)
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Amount", color = Color.Gray, fontSize = 13.sp)
                                Text(
                                    "₹${"%.2f".format(s.payload.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 18.sp,
                                    color      = Color.White
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Source", color = Color.Gray, fontSize = 13.sp)
                                Text(
                                    "Customer Web Confirmation",
                                    fontSize = 13.sp,
                                    color    = Color(0xFF22C55E)
                                )
                            }
                        }
                    }

                    Text(
                        "The customer confirmed this transaction\nvia the QR code web link.",
                        textAlign = TextAlign.Center,
                        color     = Color.Gray,
                        fontSize  = 13.sp
                    )

                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick  = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Done") }
                }

                // ── Firebase rejected ────────────────────────────────────────
                is QrDisplayState.RejectedViaFirebase -> {
                    Spacer(Modifier.weight(1f))
                    Text("🚫", fontSize = 64.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Customer Rejected",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp,
                        color      = Color(0xFFEF4444)
                    )
                    Text(
                        "The customer rejected this transaction.\nPlease verify the details with them.",
                        textAlign = TextAlign.Center,
                        color     = Color.Gray,
                        fontSize  = 13.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick  = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Go Back") }
                }

                // ── Waiting for Firebase (shown after countdown) ─────────────
                is QrDisplayState.WaitingForConfirmation -> {
                    Spacer(Modifier.weight(1f))

                    // Pulsing animation
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue  = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation  = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )

                    Box(
                        modifier         = Modifier
                            .size(80.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF6366F1), Color(0xFF818CF8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📱", fontSize = 36.sp)
                    }

                    Text(
                        "Waiting for Customer…",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp
                    )
                    Text(
                        "The QR code link has been shared.\nWaiting for the customer to confirm.",
                        textAlign = TextAlign.Center,
                        color     = Color.Gray,
                        fontSize  = 13.sp
                    )

                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = Color(0xFF6366F1)
                    )

                    Spacer(Modifier.weight(1f))
                }

                // ── QR Ready (main state) ────────────────────────────────────
                is QrDisplayState.Ready -> {
                    // ── Transaction info ─────────────────────────────────────
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Type",   color = Color.Gray, fontSize = 12.sp)
                                Text(
                                    s.payload.type,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = if (s.payload.type == "CREDIT")
                                        Color(0xFFEF4444) else Color(0xFF22C55E)
                                )
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Amount", color = Color.Gray, fontSize = 12.sp)
                                Text(
                                    "₹${"%.2f".format(s.payload.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 18.sp
                                )
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Txn ID", color = Color.Gray, fontSize = 12.sp)
                                Text(s.payload.txnId, fontSize = 12.sp)
                            }
                        }
                    }

                    // ── QR bitmap ────────────────────────────────────────────
                    Box(
                        modifier         = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap             = s.bitmap.asImageBitmap(),
                            contentDescription = "Confirmation QR code",
                            modifier           = Modifier.fillMaxSize()
                        )
                    }

                    // ── Countdown bar ────────────────────────────────────────
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val barColor = when {
                            secondsLeft > 30 -> Color(0xFF22C55E)
                            secondsLeft > 10 -> Color(0xFFF59E0B)
                            else             -> Color(0xFFEF4444)
                        }
                        LinearProgressIndicator(
                            progress           = { timerFraction },
                            modifier           = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color              = barColor,
                            trackColor         = barColor.copy(alpha = 0.2f),
                            strokeCap          = StrokeCap.Round
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Expires in ${secondsLeft}s",
                            fontSize   = 12.sp,
                            color      = barColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // ── Firebase-aware instruction text ──────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1B4B).copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "📱 Customer: Scan this QR with your phone camera",
                                textAlign  = TextAlign.Center,
                                color      = Color(0xFFA5B4FC),
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "A web page will open — no app needed!",
                                textAlign = TextAlign.Center,
                                color     = Color(0xFF94A3B8),
                                fontSize  = 11.sp
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    OutlinedButton(
                        onClick  = { confirmationVm.requestQrForTxnId(txnId) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Regenerate Fresh QR") }
                }
            }
        }
    }
}
