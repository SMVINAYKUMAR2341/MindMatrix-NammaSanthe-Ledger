package com.nammasanthe.ledger.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nammasanthe.ledger.data.entity.TxnEntity
import com.nammasanthe.ledger.data.entity.TxnType
import com.nammasanthe.ledger.security.TrustLevel
import com.nammasanthe.ledger.ui.components.TrustBadge
import com.nammasanthe.ledger.util.PhotoProofManager
import com.nammasanthe.ledger.viewmodel.ConfirmationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Transaction Detail Screen with Photo Proof for Dispute Resolution.
 * Shows complete evidence: transaction details, photo, QR confirmation status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transaction: TxnEntity,
    customerName: String,
    confirmationVm: ConfirmationViewModel,
    onBack: () -> Unit,
    onAddPhoto: (Long, Uri) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Observe confirmation status
    val confirmation by confirmationVm.observeConfirmation(transaction.id)
        .collectAsStateWithLifecycle(initialValue = null)

    // Photo picker launcher
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAddPhoto(transaction.id, it) }
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Evidence") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 🔥 DISPUTE RESOLUTION HEADER
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (confirmation != null) {
                        Color(0xFFDCFCE7) // Green for confirmed
                    } else {
                        Color(0xFFFEE2E2) // Red for unconfirmed
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (confirmation != null) "✅ VERIFIED TRANSACTION" 
                        else "⚠️ UNCONFIRMED TRANSACTION",
                        fontWeight = FontWeight.Bold,
                        color = if (confirmation != null) Color(0xFF166534) else Color(0xFF991B1B)
                    )
                    if (confirmation != null) {
                        Spacer(Modifier.height(8.dp))
                        TrustBadge(confirmation!!.trustLevel)
                    }
                }
            }

            // 📊 Transaction Details Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Transaction Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    DetailRow("Customer", customerName)
                    DetailRow(
                        "Type", 
                        if (transaction.type == TxnType.CREDIT) "Credit (Udhar)" else "Payment",
                        color = if (transaction.type == TxnType.CREDIT) Color(0xFFDC2626) else Color(0xFF16A34A)
                    )
                    DetailRow("Amount", "₹${"%.2f".format(transaction.amount)}")
                    DetailRow("Date", dateFormat.format(Date(transaction.date)))
                    DetailRow("Transaction ID", "#${transaction.id}")
                    
                    if (!transaction.note.isNullOrBlank()) {
                        DetailRow("Note", transaction.note)
                    }
                }
            }

            // 📸 PHOTO PROOF SECTION
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📸 Photo Proof",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (transaction.photoPath == null) {
                            Badge(containerColor = Color(0xFFFEF3C7)) {
                                Text("Missing", color = Color(0xFF92400E))
                            }
                        } else {
                            Badge(containerColor = Color(0xFFDCFCE7)) {
                                Text("Present", color = Color(0xFF166534))
                            }
                        }
                    }

                    if (transaction.photoPath != null && PhotoProofManager.photoExists(transaction.photoPath)) {
                        // Show captured photo
                        val bitmap = remember(transaction.photoPath) {
                            PhotoProofManager.loadPhoto(transaction.photoPath)
                        }
                        
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Transaction photo proof",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Text(
                            "Photo captured at transaction time. This serves as evidence in case of disputes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        // No photo - show add button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF3F4F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No photo proof captured",
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Text(
                            "⚠️ Adding a photo provides strong evidence in disputes. Capture items sold, signature, or scene.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E)
                        )
                        
                        OutlinedButton(
                            onClick = { photoPicker.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddAPhoto, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Photo Proof")
                        }
                    }
                }
            }

            // 📱 QR Confirmation Status
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "📱 QR Confirmation Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (confirmation != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = when (confirmation!!.trustLevel) {
                                    TrustLevel.VERIFIED -> Color(0xFF22C55E)
                                    TrustLevel.SUSPICIOUS -> Color(0xFFF59E0B)
                                    TrustLevel.UNVERIFIED -> Color(0xFF64748B)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    when (confirmation!!.trustLevel) {
                                        TrustLevel.VERIFIED -> "✅ Customer Confirmed via QR Scan"
                                        TrustLevel.SUSPICIOUS -> "⚠️ Flagged - Same Device Scan"
                                        TrustLevel.UNVERIFIED -> "ℹ️ Manual Confirmation"
                                    },
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Confirmed on: ${dateFormat.format(Date(confirmation!!.confirmedAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = Color(0xFFDC2626)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Not confirmed - Show QR to customer",
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            // 📋 Evidence Summary for Disputes
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "🛡️ Dispute Evidence Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val evidenceItems = listOf(
                        "Transaction recorded" to true,
                        "Photo proof" to (transaction.photoPath != null),
                        "QR confirmation" to (confirmation != null),
                        "Customer verification" to (confirmation?.trustLevel == TrustLevel.VERIFIED)
                    )
                    
                    evidenceItems.forEach { (item, hasIt) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (hasIt) "✅" else "❌",
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                item,
                                color = if (hasIt) Color(0xFF166534) else Color.Gray
                            )
                        }
                    }
                    
                    if (evidenceItems.count { it.second } >= 3) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "🎯 Strong evidence - Dispute unlikely to succeed against you",
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.Medium
                        )
                    } else if (evidenceItems.count { it.second } <= 1) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⚠️ Weak evidence - Capture more proof for future transactions",
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            
            // Share Evidence Button
            OutlinedButton(
                onClick = {
                    // Share functionality
                    scope.launch {
                        shareEvidence(ctx, transaction, customerName, confirmation)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Share Evidence")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, color = color, fontSize = 14.sp)
    }
}

private suspend fun shareEvidence(
    context: android.content.Context,
    transaction: TxnEntity,
    customerName: String,
    confirmation: com.nammasanthe.ledger.data.entity.ConfirmationEntity?
) = withContext(Dispatchers.IO) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val evidenceText = buildString {
        appendLine("📋 Namma Santhe Transaction Evidence")
        appendLine()
        appendLine("Customer: $customerName")
        appendLine("Amount: ₹${"%.2f".format(transaction.amount)}")
        appendLine("Type: ${if (transaction.type == TxnType.CREDIT) "Credit (Udhar)" else "Payment"}")
        appendLine("Date: ${dateFormat.format(Date(transaction.date))}")
        appendLine("Transaction ID: #${transaction.id}")
        appendLine()
        appendLine("QR Confirmation: ${if (confirmation != null) "✅ Confirmed" else "❌ Not Confirmed"}")
        if (confirmation != null) {
            appendLine("Trust Level: ${confirmation.trustLevel}")
            appendLine("Confirmed At: ${dateFormat.format(Date(confirmation.confirmedAt))}")
        }
        appendLine()
        appendLine("Photo Proof: ${if (transaction.photoPath != null) "✅ Available" else "❌ Not Available"}")
    }
    
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Transaction Evidence - ID ${transaction.id}")
        putExtra(android.content.Intent.EXTRA_TEXT, evidenceText)
    }
    
    context.startActivity(
        android.content.Intent.createChooser(intent, "Share Evidence")
    )
}
