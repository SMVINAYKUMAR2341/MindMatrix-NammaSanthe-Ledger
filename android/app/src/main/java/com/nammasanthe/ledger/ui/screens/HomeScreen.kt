package com.nammasanthe.ledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.nammasanthe.ledger.util.ContactActions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nammasanthe.ledger.ui.components.GradientCard
import com.nammasanthe.ledger.ui.components.GreetingCard
import com.nammasanthe.ledger.ui.components.SectionHeader
import com.nammasanthe.ledger.ui.components.StatTile
import com.nammasanthe.ledger.viewmodel.ProfileViewModel
import com.nammasanthe.ledger.ui.theme.*
import com.nammasanthe.ledger.viewmodel.LedgerViewModel

@Composable
fun HomeScreen(
    viewModel: LedgerViewModel,
    profileViewModel: ProfileViewModel,
    onQuickEntry: () -> Unit,
    onCustomers: () -> Unit,
    onReports: () -> Unit,
    onScanner: () -> Unit,
    onProfile: () -> Unit,
    onCustomer: (Long) -> Unit
) {
    val profile by profileViewModel.profile.collectAsStateWithLifecycle()
    val outstanding by viewModel.outstanding.collectAsStateWithLifecycle()
    val balances by viewModel.balances.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    val today = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayCredit = recent.filter { it.date >= today && it.type.name == "CREDIT" }.sumOf { it.amount }
    val todayPayment = recent.filter { it.date >= today && it.type.name == "PAYMENT" }.sumOf { it.amount }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Storefront, null, tint = Saffron)
            Spacer(Modifier.width(8.dp))
            Text("Namma Santhe Ledger", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onProfile) { Icon(Icons.Default.Settings, "Profile", tint = Saffron) }
        }
        GreetingCard(businessName = profile.businessName.ifBlank { profile.ownerName })

        Button(
            onClick = onQuickEntry,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp))
            Text("Quick Entry", fontWeight = FontWeight.SemiBold)
        }

        GradientCard(brush = PrimaryGradient) {
            Text("Total Outstanding", color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("₹${"%.2f".format(outstanding)}", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Today's Credit", "₹${"%.0f".format(todayCredit)}", Crimson, Modifier.weight(1f))
            StatTile("Today's Payment", "₹${"%.0f".format(todayPayment)}", Emerald500, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("Customers", Icons.Default.Group, Indigo600, Modifier.weight(1f), onCustomers)
            QuickAction("Reports", Icons.Default.QueryStats, Pink600, Modifier.weight(1f), onReports)
            QuickAction("Scan", Icons.Default.DocumentScanner, Saffron, Modifier.weight(1f), onScanner)
        }

        SectionHeader("Top Pending")
        val top = balances.filter { it.balance > 0 }.sortedByDescending { it.balance }.take(3)
        if (top.isEmpty()) {
            Card { Box(Modifier.padding(16.dp)) { Text("All clear — no dues 🎉") } }
        } else {
            top.forEach { b ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Text(b.name, fontWeight = FontWeight.SemiBold)
                            Text(b.phone, color = Color.Gray, fontSize = 12.sp)
                            Text("₹${"%.0f".format(b.balance)} pending", color = Crimson, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = {
                            ContactActions.whatsappReminder(ctx, b.name, b.phone, b.balance)
                        }) { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp", tint = Emerald500) }
                        IconButton(onClick = { ContactActions.call(ctx, b.phone) }) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = Indigo600)
                        }
                        IconButton(onClick = { onCustomer(b.customerId) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Open")
                        }
                    }
                }
            }
        }

        SectionHeader("Recent Transactions")
        if (recent.isEmpty()) {
            Card { Box(Modifier.padding(16.dp)) { Text("No transactions yet") } }
        } else {
            recent.take(5).forEach { t ->
                val name = customers.firstOrNull { it.id == t.customerId }?.name ?: "—"
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.SemiBold)
                            Text(t.type.name, color = Color.Gray, fontSize = 12.sp)
                        }
                        val color = if (t.type.name == "CREDIT") Crimson else Emerald500
                        Text(
                            "${if (t.type.name == "CREDIT") "+" else "-"}₹${"%.0f".format(t.amount)}",
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    OutlinedCard(onClick = onClick, modifier = modifier.height(96.dp)) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = accent)
            Spacer(Modifier.height(6.dp))
            Text(label, color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
