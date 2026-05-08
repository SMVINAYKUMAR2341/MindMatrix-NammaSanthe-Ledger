package com.nammasanthe.ledger.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nammasanthe.ledger.data.repo.AppProfile
import com.nammasanthe.ledger.sync.AuthState
import com.nammasanthe.ledger.sync.FirebaseAuthManager
import com.nammasanthe.ledger.sync.FirebaseSyncManager
import com.nammasanthe.ledger.sync.SyncState
import com.nammasanthe.ledger.util.DataExportManager
import com.nammasanthe.ledger.viewmodel.LedgerViewModel
import com.nammasanthe.ledger.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enhanced Profile Screen with Data Export, Cloud Auth, and Sync.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    ledgerViewModel: LedgerViewModel,
    onBack: () -> Unit,
    onGeminiSettings: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var local by remember(profile) { mutableStateOf(profile) }
    var pin by remember { mutableStateOf("") }

    // Auth & Sync Managers
    val authManager = remember { FirebaseAuthManager.getInstance(ctx) }
    val syncManager = remember { FirebaseSyncManager.getInstance(ctx) }
    val authState by authManager.authState.collectAsStateWithLifecycle()
    val syncState by syncManager.syncState.collectAsStateWithLifecycle()
    val currentUser by authManager.currentUser.collectAsStateWithLifecycle()
    val lastSync by syncManager.lastSyncTime.collectAsStateWithLifecycle()

    // UI States
    var showLoginDialog by remember { mutableStateOf(false) }
    var showSignupDialog by remember { mutableStateOf(false) }
    var showExportProgress by remember { mutableStateOf(false) }
    var exportFile by remember { mutableStateOf<File?>(null) }

    // Load ledger data for export
    val customers by ledgerViewModel.customers.collectAsStateWithLifecycle()
    val transactions by remember { mutableStateOf(listOf<com.nammasanthe.ledger.data.entity.TxnEntity>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Profile & Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── BUSINESS PROFILE ─────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Business Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    FieldRow("Business Name", local.businessName) { local = local.copy(businessName = it) }
                    FieldRow("Owner Name", local.ownerName) { local = local.copy(ownerName = it) }
                    FieldRow("Phone", local.phone) { local = local.copy(phone = it) }
                    FieldRow("Address", local.address) { local = local.copy(address = it) }
                    FieldRow("GST Number", local.gstNumber) { local = local.copy(gstNumber = it) }

                    Text("Language", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("en" to "English", "hi" to "हिन्दी", "kn" to "ಕನ್ನಡ").forEach { (code, label) ->
                            FilterChip(
                                selected = local.language == code,
                                onClick = { local = local.copy(language = code) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // ── CLOUD AUTH & SYNC ────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "☁️ Cloud Backup & Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    when (val state = authState) {
                        is AuthState.Authenticated -> {
                            // User is signed in
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Signed in as:", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        currentUser?.email ?: "Unknown",
                                        fontWeight = FontWeight.Medium
                                    )
                                    lastSync?.let { syncTime ->
                                        val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                                        Text(
                                            "Last sync: ${dateFormat.format(Date(syncTime))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = { authManager.signOut() },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Sign Out")
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Sync buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val allTransactions = ledgerViewModel.getAllTransactions()
                                            val result = syncManager.syncAllData(
                                                profile = local,
                                                customers = customers,
                                                transactions = allTransactions
                                            )
                                            result.onSuccess {
                                                Toast.makeText(ctx, "✅ Data backed up to cloud", Toast.LENGTH_SHORT).show()
                                            }.onFailure { e ->
                                                Toast.makeText(ctx, "❌ ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = syncState !is SyncState.Syncing
                                ) {
                                    if (syncState is SyncState.Syncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text("Backup Now")
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val result = syncManager.downloadFromCloud()
                                            result.onSuccess { cloudData ->
                                                // TODO: Merge cloud data with local
                                                Toast.makeText(ctx, "✅ Data downloaded from cloud", Toast.LENGTH_SHORT).show()
                                            }.onFailure { e ->
                                                Toast.makeText(ctx, "❌ ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = syncState !is SyncState.Syncing
                                ) {
                                    Text("Restore")
                                }
                            }
                        }

                        is AuthState.Unauthenticated -> {
                            // Not signed in
                            Text(
                                "Sign in to backup your data to the cloud. Your data will be encrypted and safe.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showLoginDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Sign In")
                                }
                                Button(
                                    onClick = { showSignupDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Create Account")
                                }
                            }
                        }

                        is AuthState.Error -> {
                            Text(
                                "Error: ${state.message}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // ── DATA EXPORT ───────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "📄 Data Export",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Export all your ledger data as a PDF file. Share via WhatsApp, Email, or save to device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                showExportProgress = true
                                val allTransactions = ledgerViewModel.getAllTransactions()
                                val file = DataExportManager.exportCompleteData(
                                    context = ctx,
                                    profile = local,
                                    customers = customers,
                                    transactions = allTransactions,
                                    getCustomerName = { customerId ->
                                        customers.find { it.id == customerId }?.name ?: "Unknown"
                                    }
                                )
                                showExportProgress = false
                                exportFile = file
                                file?.let {
                                    DataExportManager.sharePdf(ctx, it)
                                    Toast.makeText(ctx, "PDF exported successfully", Toast.LENGTH_SHORT).show()
                                } ?: Toast.makeText(ctx, "Export failed", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !showExportProgress
                    ) {
                        if (showExportProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Generating PDF...")
                        } else {
                            Icon(Icons.Default.PictureAsPdf, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export Complete Ledger as PDF")
                        }
                    }

                    exportFile?.let { file ->
                        OutlinedButton(
                            onClick = { DataExportManager.sharePdf(ctx, file) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Share Last Export")
                        }
                    }
                }
            }

            // ── OCR SETTINGS ──────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "🔮 OCR Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = onGeminiSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Configure Gemini AI (Advanced OCR)")
                    }
                }
            }

            // ── SECURITY ──────────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "🔒 Security",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text(if (profile.pinHash == null) "Set PIN" else "Change PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (pin.length in 4..6) {
                                    viewModel.setPin(pin)
                                    pin = ""
                                    Toast.makeText(ctx, "PIN saved", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = pin.length in 4..6
                        ) { Text("Save PIN") }

                        if (profile.pinHash != null) {
                            OutlinedButton(
                                onClick = { viewModel.setPin(null) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("Remove PIN") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Save Profile Button
            Button(
                onClick = {
                    viewModel.save(local)
                    Toast.makeText(ctx, "Profile saved", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save All Settings")
            }
        }
    }

    // Login Dialog
    if (showLoginDialog) {
        AuthDialog(
            title = "Sign In",
            onDismiss = { showLoginDialog = false },
            onSubmit = { email, password ->
                scope.launch {
                    val result = authManager.signIn(email, password)
                    result.onSuccess {
                        Toast.makeText(ctx, "Signed in successfully", Toast.LENGTH_SHORT).show()
                        showLoginDialog = false
                    }.onFailure { e ->
                        Toast.makeText(ctx, e.message ?: "Error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Signup Dialog
    if (showSignupDialog) {
        AuthDialog(
            title = "Create Account",
            onDismiss = { showSignupDialog = false },
            onSubmit = { email, password ->
                scope.launch {
                    val result = authManager.signUp(email, password)
                    result.onSuccess {
                        Toast.makeText(ctx, "Account created", Toast.LENGTH_SHORT).show()
                        showSignupDialog = false
                    }.onFailure { e ->
                        Toast.makeText(ctx, e.message ?: "Error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun AuthDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(email, password) },
                enabled = email.isNotBlank() && password.length >= 6
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun FieldRow(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = label != "Address"
    )
}

