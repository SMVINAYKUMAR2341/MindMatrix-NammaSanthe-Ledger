package com.nammasanthe.ledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nammasanthe.ledger.ocr.BillItem
import com.nammasanthe.ledger.ocr.OcrLanguage
import com.nammasanthe.ledger.viewmodel.OcrViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    ocrViewModel : OcrViewModel,
    onBack       : () -> Unit,
    onRetry      : () -> Unit
) {
    val state by ocrViewModel.state.collectAsStateWithLifecycle()
    val showLanguageDialog = remember { mutableStateOf(false) }
    val currentLanguage = remember { mutableStateOf(OcrLanguage.UNKNOWN) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Result") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Progress + status chips ──────────────────────────────────────
            if (state.processing) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let   { Text("Error: $it",   color = Color(0xFFDC2626)) }
            state.warning?.let { Text("Note: $it",    color = Color(0xFF8A5A00)) }

            AssistChipRow(
                "Lang: ${state.language}",
                "Engine: ${state.source.ifBlank { "—" }}",
                "Conf: ${"%.0f".format(state.confidence * 100)}%"
            )

            // Show Gemini indicator if using Gemini API
            if (state.usingGemini) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4285F4).copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔮 Using Gemini AI for OCR", color = Color(0xFF4285F4))
                    }
                }
            }

            // Language selection button
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showLanguageDialog.value = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Language, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Change OCR Language")
                }
            }

            state.amount?.let {
                Text("Detected Amount: ₹${"%.2f".format(it)}", fontWeight = FontWeight.SemiBold)
            }

            // ── Parsed Bill Card (shown when items are available) ────────────
            if (state.parsedItems.isNotEmpty()) {
                ParsedBillCard(
                    items     = state.parsedItems,
                    onReparse = ocrViewModel::parseBill
                )
            } else if (state.translated.isNotBlank()) {
                // Show enhanced parsing option when no items but text exists
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "No bill items detected from OCR text",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8A5A00)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Try parsing manually:", fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = ocrViewModel::parseBill,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📋 Parse Bill Items")
                        }
                    }
                }
            }

            // ── Raw OCR (collapsed by default when items exist) ──────────────
            if (state.parsedItems.isEmpty()) {
                Text("Kannada (Raw OCR)", fontWeight = FontWeight.SemiBold)
                OutlinedCard {
                    OutlinedTextField(
                        value         = state.original,
                        onValueChange = ocrViewModel::updateOriginal,
                        modifier      = Modifier.fillMaxWidth().height(120.dp),
                        placeholder   = { Text("OCR text appears here") }
                    )
                }
            }

            // ── Translated (collapsed by default when items exist) ────────────
            if (state.parsedItems.isEmpty()) {
                Text("English (Translated)", fontWeight = FontWeight.SemiBold)
                OutlinedCard {
                    OutlinedTextField(
                        value         = state.translated,
                        onValueChange = ocrViewModel::updateTranslated,
                        modifier      = Modifier.fillMaxWidth().height(120.dp),
                        placeholder   = { Text("English translation appears here") }
                    )
                }
            }

            // ── Action row 1 ─────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = onRetry,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Retry OCR")
                }
                OutlinedButton(
                    onClick  = ocrViewModel::retranslate,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Translate, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Translate Again")
                }
            }

            // ── Action row 2 ─────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = ocrViewModel::enhance,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Enhance")
                }
                Button(
                    onClick  = { ocrViewModel.saveScan(); onBack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save")
                }
            }

            // Show Re-Parse if items are absent but text exists
            if (state.parsedItems.isEmpty() && state.translated.isNotBlank()) {
                OutlinedButton(
                    onClick  = ocrViewModel::parseBill,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📋  Parse Bill Items")
                }
            }
        }
    }

    // Language selection dialog
    if (showLanguageDialog.value) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog.value = false },
            title = { Text("Select OCR Language") },
            text = {
                Column {
                    OcrLanguage.values().filter { it != OcrLanguage.UNKNOWN }.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentLanguage.value = language
                                    ocrViewModel.setPreferredLanguage(language)
                                    showLanguageDialog.value = false
                                    onRetry() // Retry OCR with new language
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage.value == language,
                                onClick = {
                                    currentLanguage.value = language
                                    ocrViewModel.setPreferredLanguage(language)
                                    showLanguageDialog.value = false
                                    onRetry() // Retry OCR with new language
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when (language) {
                                    OcrLanguage.ENGLISH -> "English"
                                    OcrLanguage.HINDI -> "हिंदी (Hindi)"
                                    OcrLanguage.KANNADA -> "ಕನ್ನಡ (Kannada)"
                                    OcrLanguage.UNKNOWN -> "Auto-detect"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Parsed Bill Card ──────────────────────────────────────────────────────────

@Composable
private fun ParsedBillCard(
    items    : List<BillItem>,
    onReparse: () -> Unit
) {
    val total = items.sumOf { it.amount }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {

            // Header
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("📋 Parsed Bill Items", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                TextButton(onClick = onReparse) { Text("Re-Parse", fontSize = 12.sp) }
            }

            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            // Column headers
            BillRow(
                item     = "Item",
                quantity = "Qty",
                amount   = "Amount",
                isHeader = true
            )

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Data rows
            items.forEachIndexed { idx, item ->
                BillRow(
                    item     = item.item,
                    quantity = item.quantity,
                    amount   = if (item.amount > 0) "₹${item.amount}" else "—",
                    isHeader = false,
                    dimmed   = item.confidence < 0.45f
                )
                if (idx < items.lastIndex) {
                    HorizontalDivider(
                        color     = Color(0xFFF1F5F9),
                        thickness = 0.5.dp
                    )
                }
            }

            // Total row
            if (total > 0) {
                HorizontalDivider()
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Total",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp
                    )
                    Text(
                        "₹$total",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        color      = Color(0xFF22C55E)
                    )
                }
            }

            // Low-confidence note
            val lowConfCount = items.count { it.confidence < 0.45f }
            if (lowConfCount > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "⚠ $lowConfCount item(s) have low parse confidence — verify manually.",
                    fontSize = 11.sp,
                    color    = Color(0xFFF59E0B)
                )
            }
        }
    }
}

@Composable
private fun BillRow(
    item    : String,
    quantity: String,
    amount  : String,
    isHeader: Boolean,
    dimmed  : Boolean = false
) {
    val weight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal
    val alpha  = if (dimmed) 0.5f else 1f
    val color  = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isHeader) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item,     modifier = Modifier.weight(2f), fontWeight = weight, fontSize = 13.sp, color = color)
        Text(
            quantity,
            modifier   = Modifier.weight(1f),
            fontWeight = weight,
            fontSize   = 13.sp,
            color      = if (quantity == "unknown") Color(0xFF94A3B8) else color,
            textAlign  = TextAlign.Center
        )
        Text(
            amount,
            modifier   = Modifier.weight(1f),
            fontWeight = if (isHeader) weight else FontWeight.SemiBold,
            fontSize   = 13.sp,
            color      = if (!isHeader && amount != "—") Color(0xFF1E40AF) else color,
            textAlign  = TextAlign.End
        )
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun AssistChipRow(vararg labels: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
    }
}
