package com.nammasanthe.ledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun GreetingCard(businessName: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var streak by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val prefs = ctx.getSharedPreferences("greeting", android.content.Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val last = prefs.getString("last", null)
        var s = prefs.getInt("streak", 0)
        if (last != today) {
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val y = SimpleDateFormat("yyyyMMdd", Locale.US).format(yesterday.time)
            s = if (last == y) s + 1 else 1
            prefs.edit().putString("last", today).putInt("streak", s).apply()
        }
        streak = s
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val (greet, gradient) = when (hour) {
        in 5..11 -> "Good Morning" to Brush.linearGradient(
            listOf(Color(0xFFFCBF49), Color(0xFFF77F00), Color(0xFFE63946))
        )
        in 12..16 -> "Good Afternoon" to Brush.linearGradient(
            listOf(Color(0xFFF77F00), Color(0xFFE63946), Color(0xFFC9184A))
        )
        in 17..19 -> "Good Evening" to Brush.linearGradient(
            listOf(Color(0xFFE63946), Color(0xFFB5179E), Color(0xFF7209B7))
        )
        else -> "Good Night" to Brush.linearGradient(
            listOf(Color(0xFF3A0CA3), Color(0xFF7209B7), Color(0xFFB5179E))
        )
    }

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbSunny, null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(greet, color = Color.White)
                }
                Text(
                    "${businessName.ifBlank { "Vendor" }} Ji 🙏",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                val date = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()).format(Date())
                Text(date, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
            if (streak > 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFFE066))
                    Text("$streak", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("streak", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                }
            }
        }
    }
}
