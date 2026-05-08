package com.nammasanthe.ledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Saffron = Color(0xFFFF7A1A)
val Crimson = Color(0xFFE63946)
val Pink600 = Color(0xFFDB2777)
val Indigo600 = Color(0xFF4F46E5)
val Amber500 = Color(0xFFF59E0B)
val Emerald500 = Color(0xFF10B981)

val PrimaryGradient = Brush.linearGradient(listOf(Saffron, Crimson, Pink600))
val InfoGradient = Brush.linearGradient(listOf(Indigo600, Pink600))
val WarmGradient = Brush.linearGradient(listOf(Amber500, Saffron))

private val LightColors = lightColorScheme(
    primary = Saffron,
    secondary = Pink600,
    tertiary = Indigo600
)
private val DarkColors = darkColorScheme(
    primary = Saffron,
    secondary = Pink600,
    tertiary = Indigo600
)

@Composable
fun NammaSantheTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
