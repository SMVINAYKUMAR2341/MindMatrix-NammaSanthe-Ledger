package com.nammasanthe.ledger.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nammasanthe.ledger.security.TrustLevel

/**
 * Small pill badge that visualises the confirmation trust level of a transaction.
 *
 *  VERIFIED   → green
 *  SUSPICIOUS → red / amber
 *  UNVERIFIED → grey
 *  null       → grey "Unverified" (not yet confirmed)
 */
@Composable
fun TrustBadge(level: TrustLevel?, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (level) {
        TrustLevel.VERIFIED   -> Triple("✓ Verified",    Color(0xFF22C55E), Color(0xFF166534))
        TrustLevel.SUSPICIOUS -> Triple("⚠ Suspicious",  Color(0xFFEF4444), Color(0xFF7F1D1D))
        else                  -> Triple("◌ Unverified",  Color(0xFF94A3B8), Color(0xFF334155))
    }
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(20.dp),
        color    = bg.copy(alpha = 0.15f)
    ) {
        Text(
            text       = label,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color      = fg
        )
    }
}
