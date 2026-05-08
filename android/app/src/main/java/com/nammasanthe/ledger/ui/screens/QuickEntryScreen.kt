package com.nammasanthe.ledger.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nammasanthe.ledger.data.entity.TxnType
import com.nammasanthe.ledger.viewmodel.LedgerViewModel
import com.nammasanthe.ledger.voice.VoiceEvent
import com.nammasanthe.ledger.voice.VoiceInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEntryScreen(viewModel: LedgerViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TxnType.CREDIT) }
    var partial by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val voice = remember { VoiceInput(ctx) }

    // Check permission state
    val hasAudioPermission = remember {
        ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Start voice recognition directly
            scope.launch {
                isListening = true
                errorMessage = null
                partial = "Listening..."
                
                try {
                    voice.listen("en-US").collect { event ->
                        when (event) {
                            is VoiceEvent.Partial -> {
                                partial = event.text
                                errorMessage = null
                            }
                            is VoiceEvent.Final -> {
                                isListening = false
                                partial = event.command.rawText
                                event.command.amount?.let { amount = it.toString() }
                                event.command.type?.let { type = it }
                                val name = event.command.customerName
                                if (name != null) {
                                    selectedId = customers.firstOrNull { it.name.equals(name, true) }?.id
                                }
                            }
                            is VoiceEvent.Error -> {
                                isListening = false
                                errorMessage = when (event.code) {
                                    -1 -> "Speech recognition not available on this device"
                                    else -> "Voice recognition error: ${event.code}"
                                }
                                partial = ""
                            }
                        }
                    }
                } catch (e: Exception) {
                    isListening = false
                    errorMessage = "Failed to start voice recognition: ${e.message}"
                    partial = ""
                }
            }
        } else {
            errorMessage = "Microphone permission required for voice input"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Entry") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(type == TxnType.CREDIT, { type = TxnType.CREDIT }, label = { Text("Credit") })
                FilterChip(type == TxnType.PAYMENT, { type = TxnType.PAYMENT }, label = { Text("Payment") })
            }
            Spacer(Modifier.height(12.dp))
            Text("Customer", fontWeight = FontWeight.SemiBold)
            ExposedDropdown(
                customers = customers,
                selectedId = selectedId,
                onSelect = { selectedId = it }
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                enabled = !isListening,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Mic, null); Spacer(Modifier.width(8.dp))
                Text(if (isListening) "Listening..." else "Speak (e.g. \"Ramesh 200 credit\")")
            }
            if (partial.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(partial, fontSize = 14.sp)
            }
            errorMessage?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(error, fontSize = 12.sp, color = Color.Red)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val cid = selectedId ?: return@Button
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    viewModel.addTransaction(cid, type, amt)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdown(
    customers: List<com.nammasanthe.ledger.data.entity.Customer>,
    selectedId: Long?,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val name = customers.firstOrNull { it.id == selectedId }?.name ?: "Select customer"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = name, onValueChange = {}, readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            customers.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.name) },
                    onClick = { onSelect(c.id); expanded = false }
                )
            }
        }
    }
}
