package com.nammasanthe.ledger.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nammasanthe.ledger.data.dao.CustomerBalance
import com.nammasanthe.ledger.data.entity.TxnType
import com.nammasanthe.ledger.ui.theme.Crimson
import com.nammasanthe.ledger.ui.theme.Emerald500
import com.nammasanthe.ledger.viewmodel.LedgerViewModel
import kotlin.math.abs

// ─── palette ─────────────────────────────────────────────────────────────────
private val Blue    = Color(0xFF3B82F6)
private val Amber   = Color(0xFFF59E0B)
private val Grid    = Color(0xFFE2E8F0)
private val ZeroRef = Color(0xFF94A3B8)

// ─── per-day bucket ───────────────────────────────────────────────────────────
private data class DayData(
    val timestamp: Long,
    val credit: Double,
    val payment: Double
) {
    val net: Double get() = credit - payment
}

// ═════════════════════════════════════════════════════════════════════════════
// Screen
// ═════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: LedgerViewModel, onBack: () -> Unit) {

    var periodDays by remember { mutableIntStateOf(7) }
    val since = remember(periodDays) {
        System.currentTimeMillis() - periodDays.toLong() * 86_400_000L
    }

    // collect transactions for the chosen period
    val txnsFlow = remember(since) { viewModel.transactionsSince(since) }
    val allTxns by txnsFlow.collectAsStateWithLifecycle(emptyList())

    val outstanding by viewModel.outstanding.collectAsStateWithLifecycle()
    val balances    by viewModel.balances.collectAsStateWithLifecycle()

    // bucket into days
    val dailyData = remember(allTxns, periodDays, since) {
        (0 until periodDays).map { offset ->
            val dayStart = since + offset.toLong() * 86_400_000L
            val dayEnd   = dayStart + 86_400_000L
            val slice    = allTxns.filter { it.date in dayStart until dayEnd }
            DayData(
                timestamp = dayStart,
                credit    = slice.filter { it.type == TxnType.CREDIT  }.sumOf { it.amount },
                payment   = slice.filter { it.type == TxnType.PAYMENT }.sumOf { it.amount }
            )
        }
    }

    val totalCredit  = dailyData.sumOf { it.credit }
    val totalPayment = dailyData.sumOf { it.payment }
    val txnCount     = allTxns.size
    val collRate     = if (outstanding + totalPayment > 0)
        totalPayment / (outstanding + totalPayment) * 100 else 0.0
    val topDebtors   = balances.filter { it.balance > 0 }.take(6)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.padding(padding).fillMaxSize(),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Period selector ──────────────────────────────────────────────
            item {
                PeriodSelector(selected = periodDays, onSelect = { periodDays = it })
            }

            // ── KPI row 1 ────────────────────────────────────────────────────
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        label = "Outstanding",
                        value = "₹${"%.0f".format(outstanding)}",
                        color = Crimson,
                        icon  = "🔴",
                        mod   = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = "Collected",
                        value = "₹${"%.0f".format(totalPayment)}",
                        color = Emerald500,
                        icon  = "✅",
                        mod   = Modifier.weight(1f)
                    )
                }
            }

            // ── KPI row 2 ────────────────────────────────────────────────────
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        label = "Credit Given",
                        value = "₹${"%.0f".format(totalCredit)}",
                        color = Blue,
                        icon  = "📦",
                        mod   = Modifier.weight(1f),
                        sub   = "Last $periodDays days"
                    )
                    KpiCard(
                        label = "Transactions",
                        value = "$txnCount",
                        color = Amber,
                        icon  = "🧾",
                        mod   = Modifier.weight(1f),
                        sub   = "Last $periodDays days"
                    )
                }
            }

            // ── Bar chart ────────────────────────────────────────────────────
            item {
                ReportCard(
                    title    = "Daily Credit vs Payment",
                    subtitle = "Red = credit given · Green = payment received"
                ) {
                    val isEmpty = dailyData.all { it.credit == 0.0 && it.payment == 0.0 }
                    if (isEmpty) EmptyMsg()
                    else         BarChart(dailyData = dailyData, periodDays = periodDays)
                }
            }

            // ── Line chart ───────────────────────────────────────────────────
            item {
                ReportCard(
                    title    = "Daily Net Position",
                    subtitle = "Credit − Payment per day (above zero = more credit given)"
                ) {
                    val isEmpty = dailyData.all { it.net == 0.0 }
                    if (isEmpty) EmptyMsg()
                    else         NetLineChart(dailyData = dailyData)
                }
            }

            // ── Donut chart ──────────────────────────────────────────────────
            item {
                ReportCard(title = "Outstanding vs Collected") {
                    DonutSection(
                        outstanding = outstanding,
                        collected   = totalPayment,
                        rate        = collRate
                    )
                }
            }

            // ── Top debtors ──────────────────────────────────────────────────
            item {
                ReportCard(
                    title    = "Top Debtors",
                    subtitle = "Customers with highest pending balance"
                ) {
                    if (topDebtors.isEmpty())
                        Text(
                            "🎉 No pending dues!",
                            modifier  = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            color     = Color.Gray
                        )
                    else
                        TopDebtorsChart(balances = topDebtors)
                }
            }

            // ── Summary table ────────────────────────────────────────────────
            item {
                SummaryCard(
                    totalCredit  = totalCredit,
                    totalPayment = totalPayment,
                    txnCount     = txnCount,
                    periodDays   = periodDays,
                    dailyData    = dailyData
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Period selector
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PeriodSelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(7, 15, 30).forEach { d ->
            FilterChip(
                selected = (selected == d),
                onClick  = { onSelect(d) },
                label    = { Text("${d}d") }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// KPI card
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun KpiCard(
    label : String,
    value : String,
    color : Color,
    icon  : String,
    mod   : Modifier = Modifier,
    sub   : String?  = null
) {
    Card(mod) {
        Row(
            modifier            = Modifier.padding(12.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 11.sp, color = Color.Gray)
                Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
                if (sub != null) Text(sub, fontSize = 10.sp, color = Color.Gray)
            }
            Text(icon, fontSize = 22.sp)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Report card wrapper
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun ReportCard(
    title    : String,
    subtitle : String? = null,
    content  : @Composable ColumnScope.() -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (subtitle != null)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun EmptyMsg() {
    Text(
        "No data for this period",
        modifier  = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        textAlign = TextAlign.Center,
        color     = Color.Gray,
        fontSize  = 13.sp
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// Bar chart  (side-by-side credit / payment per day)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun BarChart(dailyData: List<DayData>, periodDays: Int) {
    val crimson = Crimson
    val emerald = Emerald500

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val padLeft   = 8.dp.toPx()
        val padBottom = 24.dp.toPx()
        val chartW    = size.width  - padLeft
        val chartH    = size.height - padBottom

        val maxVal = (dailyData.flatMap { listOf(it.credit, it.payment) }
            .maxOrNull() ?: 1.0).coerceAtLeast(1.0)

        val n          = dailyData.size
        val groupW     = chartW / n
        val barPad     = groupW * 0.08f
        val barW       = (groupW - barPad * 3f) / 2f

        // grid lines
        val gridSteps = 4
        repeat(gridSteps + 1) { i ->
            val y = chartH * (1f - i.toFloat() / gridSteps)
            drawLine(Grid, Offset(padLeft, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        // bars
        dailyData.forEachIndexed { idx, day ->
            val xBase = padLeft + idx * groupW + barPad

            val cH = (day.credit  / maxVal).toFloat() * chartH
            val pH = (day.payment / maxVal).toFloat() * chartH

            if (cH > 1f) {
                drawRoundRect(
                    color        = crimson,
                    topLeft      = Offset(xBase, chartH - cH),
                    size         = Size(barW, cH),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
            if (pH > 1f) {
                drawRoundRect(
                    color        = emerald,
                    topLeft      = Offset(xBase + barW + barPad, chartH - pH),
                    size         = Size(barW, pH),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
        }

        // baseline
        drawLine(
            color       = Grid,
            start       = Offset(padLeft, chartH),
            end         = Offset(size.width, chartH),
            strokeWidth = 1.5.dp.toPx()
        )
    }

    // legend
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        LegendDot(Crimson,   "Credit")
        Spacer(Modifier.width(20.dp))
        LegendDot(Emerald500, "Payment")
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Net position line chart
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun NetLineChart(dailyData: List<DayData>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val padLeft   = 8.dp.toPx()
        val padBottom = 16.dp.toPx()
        val chartW    = size.width  - padLeft
        val chartH    = size.height - padBottom
        val midY      = chartH / 2f

        val maxAbs = (dailyData.map { abs(it.net) }.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        val n      = dailyData.size

        // grid
        listOf(0.0f, 0.5f, 1.0f).forEach { frac ->
            drawLine(Grid, Offset(padLeft, chartH * frac), Offset(size.width, chartH * frac), 1.dp.toPx())
        }

        // dashed zero line
        var dx = padLeft
        while (dx < size.width) {
            drawLine(
                color       = ZeroRef,
                start       = Offset(dx, midY),
                end         = Offset((dx + 8.dp.toPx()).coerceAtMost(size.width), midY),
                strokeWidth = 1.5.dp.toPx()
            )
            dx += 12.dp.toPx()
        }

        // line
        if (n > 1) {
            val xStep = chartW / (n - 1).toFloat()
            val path = Path()
            dailyData.forEachIndexed { i, d ->
                val x = padLeft + i * xStep
                val y = midY - (d.net / maxAbs).toFloat() * midY
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path  = path,
                color = Blue,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // dots
            dailyData.forEachIndexed { i, d ->
                val x = padLeft + i * xStep
                val y = midY - (d.net / maxAbs).toFloat() * midY
                drawCircle(Blue,        radius = 4.dp.toPx(), center = Offset(x, y))
                drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(x, y))
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        LegendDot(Blue, "Net (Credit − Payment)")
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Donut chart section
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun DonutSection(outstanding: Double, collected: Double, rate: Double) {
    val crimson = Crimson
    val emerald = Emerald500

    Row(
        modifier            = Modifier.fillMaxWidth(),
        verticalAlignment   = Alignment.CenterVertically
    ) {
        // donut
        Box(
            modifier         = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 26.dp.toPx()
                val inset  = stroke / 2f + 4.dp.toPx()
                val arcBox = Size(size.width - inset * 2, size.height - inset * 2)
                val tl     = Offset(inset, inset)
                val total  = outstanding + collected

                if (total <= 0.0) {
                    drawArc(Grid, 0f, 360f, false, tl, arcBox,
                        style = Stroke(stroke))
                } else {
                    val collAngle = (collected   / total * 360f).toFloat()
                    val outAngle  = (outstanding / total * 360f).toFloat()
                    // collected arc (green) from top
                    if (collAngle > 0f) {
                        drawArc(emerald, -90f, collAngle, false, tl, arcBox,
                            style = Stroke(stroke, cap = StrokeCap.Butt))
                    }
                    // outstanding arc (red)
                    if (outAngle > 0f) {
                        drawArc(crimson, -90f + collAngle, outAngle, false, tl, arcBox,
                            style = Stroke(stroke, cap = StrokeCap.Butt))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${"%.0f".format(rate)}%",   fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Emerald500)
                Text("collected", fontSize = 10.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.width(20.dp))

        // labels
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AmountLabel("● Outstanding",  "₹${"%.0f".format(outstanding)}", Crimson)
            AmountLabel("● Collected",    "₹${"%.0f".format(collected)}",   Emerald500)
            HorizontalDivider()
            AmountLabel("Total Business", "₹${"%.0f".format(outstanding + collected)}", Color.Gray)
        }
    }
}

@Composable
private fun AmountLabel(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 11.sp, color = color)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Top debtors  (Compose layout bars — no Canvas needed)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun TopDebtorsChart(balances: List<CustomerBalance>) {
    val maxBal = (balances.maxOfOrNull { it.balance } ?: 1.0).coerceAtLeast(1.0)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        balances.forEachIndexed { i, b ->
            val frac      = (b.balance / maxBal).toFloat().coerceIn(0.02f, 1f)
            val barAlpha  = 1f - i * 0.10f
            val barColor  = Crimson.copy(alpha = barAlpha)

            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // name
                Text(
                    text     = if (b.name.length > 11) b.name.take(11) + "…" else b.name,
                    modifier = Modifier.width(84.dp),
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Spacer(Modifier.width(6.dp))
                // track + fill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF1F5F9))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(frac)
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                }
                Spacer(Modifier.width(8.dp))
                // amount
                Text(
                    text       = "₹${"%.0f".format(b.balance)}",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Crimson,
                    modifier   = Modifier.width(64.dp),
                    textAlign  = TextAlign.End
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Summary stats card
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun SummaryCard(
    totalCredit  : Double,
    totalPayment : Double,
    txnCount     : Int,
    periodDays   : Int,
    dailyData    : List<DayData>
) {
    val activeDays = dailyData.count { it.credit > 0 || it.payment > 0 }
    val avgCredit  = if (activeDays > 0) totalCredit  / activeDays else 0.0
    val avgPayment = if (activeDays > 0) totalPayment / activeDays else 0.0
    val net        = totalPayment - totalCredit

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text("Summary — Last $periodDays Days",
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))

            listOf(
                Triple("Total Transactions",      "$txnCount",                              Color.Unspecified),
                Triple("Total Credit Given",      "₹${"%.0f".format(totalCredit)}",         Crimson),
                Triple("Total Payments Received", "₹${"%.0f".format(totalPayment)}",        Emerald500),
                Triple("Net Change",              "${if (net >= 0) "+" else ""}₹${"%.0f".format(net)}", if (net >= 0) Emerald500 else Crimson),
                Triple("Avg Daily Credit",        "₹${"%.0f".format(avgCredit)}",           Color.Gray),
                Triple("Avg Daily Payment",       "₹${"%.0f".format(avgPayment)}",          Color.Gray),
                Triple("Active Days",             "$activeDays / $periodDays",              Color.Gray)
            ).forEach { (label, value, color) ->
                Row(
                    modifier           = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, fontSize = 13.sp, color = Color.Gray)
                    Text(
                        value,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (color == Color.Unspecified)
                            MaterialTheme.colorScheme.onSurface else color
                    )
                }
                HorizontalDivider(color = Grid)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Shared helpers
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}
