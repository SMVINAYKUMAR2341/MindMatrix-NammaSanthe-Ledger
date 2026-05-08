package com.nammasanthe.ledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nammasanthe.ledger.viewmodel.LedgerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(viewModel: LedgerViewModel, onCustomer: (Long) -> Unit) {
    val balances by viewModel.balances.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    val filtered = balances.filter {
        query.isBlank() || it.name.contains(query, true) || it.phone.contains(query)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search customers...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.customerId }) { b ->
                    Card(onClick = { onCustomer(b.customerId) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(b.name, fontWeight = FontWeight.SemiBold)
                                Text(b.phone, color = Color.Gray)
                            }
                            val text = when {
                                b.balance > 0 -> "₹${"%.0f".format(b.balance)} pending"
                                b.balance < 0 -> "₹${"%.0f".format(-b.balance)} advance"
                                else -> "Clear"
                            }
                            Text(text, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) AddCustomerDialog(onDismiss = { showAdd = false }) { name, phone, address ->
        viewModel.addCustomer(name, phone, address)
        showAdd = false
    }
}

@Composable
private fun AddCustomerDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name *") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onAdd(name.trim(), phone.trim(), address.trim()) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
