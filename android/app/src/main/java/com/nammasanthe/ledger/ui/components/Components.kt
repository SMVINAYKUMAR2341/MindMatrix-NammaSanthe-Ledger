package com.nammasanthe.ledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GradientCard(
    brush: Brush,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        color = Color.Transparent
    ) {
        Box(
            Modifier.background(brush, RoundedCornerShape(20.dp)).fillMaxWidth().padding(20.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun StatTile(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = accent, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun PillButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp)
    ) { Text(label) }
}
