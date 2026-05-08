package com.nammasanthe.ledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nammasanthe.ledger.NammaSantheApp
import com.nammasanthe.ledger.util.ContactActions
import com.nammasanthe.ledger.util.ImageStore
import com.nammasanthe.ledger.util.PdfInvoice
import com.nammasanthe.ledger.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nammasanthe.ledger.data.entity.Customer
import com.nammasanthe.ledger.data.entity.TxnEntity
import com.nammasanthe.ledger.data.entity.TxnType
import com.nammasanthe.ledger.ui.theme.Crimson
import com.nammasanthe.ledger.ui.theme.Emerald500
import com.nammasanthe.ledger.viewmodel.LedgerViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.nammasanthe.ledger.data.entity.ConfirmationEntity
import com.nammasanthe.ledger.security.TrustLevel
import com.nammasanthe.ledger.ui.components.TrustBadge
import com.nammasanthe.ledger.viewmodel.ConfirmationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    viewModel         : LedgerViewModel,
    profileViewModel  : ProfileViewModel,
    confirmationVm    : ConfirmationViewModel,
    customerId        : Long,
    onBack            : () -> Unit,
    onShowQr          : (txnId: Long) -> Unit,
    onTransactionClick: (txnId: Long) -> Unit = {}
) {
    var customer by remember { mutableStateOf<Customer?>(null) }
    var transactions by remember { mutableStateOf<List<TxnEntity>>(emptyList()) }

    LaunchedEffect(customerId) {
        customer = viewModel.getCustomer(customerId)
        viewModel.customerTransactions(customerId).collectLatest { transactions = it }
    }

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TxnType.CREDIT) }
    var lastAddedTxnId by remember { mutableStateOf<Long?>(null) }

    val ctx = LocalContext.current
    val profile by profileViewModel.profile.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val c = customer ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                val tmp = File(ctx.cacheDir, "pick_${System.currentTimeMillis()}.jpg")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    tmp.outputStream().use { input.copyTo(it) }
                }
                ImageStore.saveCompressed(ctx, tmp)
            }
            NammaSantheApp.instance.repository.updateCustomer(c.copy(photoPath = saved))
            customer = c.copy(photoPath = saved)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "Customer") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    val c = customer
                    if (c != null) {
                        val balance = transactions.sumOf {
                            if (it.type == TxnType.CREDIT) it.amount else -it.amount
                        }
                        IconButton(onClick = {
                            ContactActions.whatsappReminder(ctx, c.name, c.phone, balance)
                        }) { Icon(Icons.AutoMirrored.Filled.Chat, "WhatsApp", tint = Emerald500) }
                        IconButton(onClick = { ContactActions.call(ctx, c.phone) }) {
                            Icon(Icons.Default.Call, "Call")
                        }
                        IconButton(onClick = {
                            scope.launch {
                                val pdf = withContext(Dispatchers.IO) {
                                    PdfInvoice.generate(ctx, profile, c, transactions)
                                }
                                PdfInvoice.share(ctx, pdf)
                            }
                        }) { Icon(Icons.Default.PictureAsPdf, "Invoice") }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val photo = customer?.photoPath
                if (photo != null) {
                    AsyncImage(
                        model = File(photo),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                    )
                } else {
                    Box(
                        Modifier.size(64.dp).clip(CircleShape)
                            .background(Color(0xFFFFE0CC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (customer?.name?.firstOrNull()?.uppercase() ?: "?"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(customer?.name ?: "—", fontWeight = FontWeight.SemiBold)
                    Text(customer?.phone.orEmpty(), color = Color.Gray, fontSize = 12.sp)
                    if (!customer?.address.isNullOrBlank()) {
                        Text(
                            customer!!.address,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                }
                OutlinedButton(onClick = { photoPicker.launch("image/*") }) {
                    Icon(Icons.Default.Photo, null); Spacer(Modifier.width(4.dp)); Text("Photo")
                }
            }
            Spacer(Modifier.height(12.dp))
            val balance = transactions.sumOf {
                if (it.type == TxnType.CREDIT) it.amount else -it.amount
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Balance", color = Color.Gray)
                    Text(
                        "₹${"%.2f".format(balance)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (balance > 0) Crimson else Emerald500
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == TxnType.CREDIT,
                    onClick = { type = TxnType.CREDIT },
                    label = { Text("Credit") }
                )
                FilterChip(
                    selected = type == TxnType.PAYMENT,
                    onClick = { type = TxnType.PAYMENT },
                    label = { Text("Payment") }
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val a = amount.toDoubleOrNull() ?: return@Button
                    scope.launch {
                        val id = viewModel.addTransactionGetId(customerId, type, a, note.ifBlank { null })
                        lastAddedTxnId = id
                        amount = ""
                        note   = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add Transaction") }

            // QR Confirmation actions — show QR for customer to scan
            lastAddedTxnId?.let { tid ->
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick  = { onShowQr(tid) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("📱 Show QR for Customer Confirmation", fontSize = 12.sp) }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(transactions, key = { it.id }) { t ->
                    Card(
                        onClick = { onTransactionClick(t.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                    val confirmation by confirmationVm
                                        .observeConfirmation(t.id)
                                        .collectAsStateWithLifecycle(null)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(t.type.name, fontWeight = FontWeight.SemiBold)
                                        TrustBadge(confirmation?.trustLevel)
                                        // Photo proof indicator
                                        if (t.photoPath != null) {
                                            Text("📸", fontSize = 12.sp)
                                        }
                                    }
                                Text(
                                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                        .format(Date(t.date)),
                                    color = Color.Gray, fontSize = 12.sp
                                )
                                t.note?.let { Text(it, fontSize = 12.sp) }
                            }
                            Text(
                                "${if (t.type == TxnType.CREDIT) "+" else "-"}₹${"%.0f".format(t.amount)}",
                                color = if (t.type == TxnType.CREDIT) Crimson else Emerald500,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
