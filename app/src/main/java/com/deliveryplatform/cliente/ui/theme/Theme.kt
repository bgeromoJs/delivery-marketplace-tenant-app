package com.deliveryplatform.cliente.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF232323)
val Muted = Color(0xFF6B6B6B)
val Canvas = Color(0xFFFAF8F6)
val Soft = Color(0xFFF3EFEA)
val Border = Color(0xFFEDE7E0)
val Success = Color(0xFF0F7B45)
val SuccessSoft = Color(0xFFE4F6EC)
val Warning = Color(0xFF8A5A00)
val WarningSoft = Color(0xFFFFF3D6)
val Danger = Color(0xFFB3141F)
val DangerSoft = Color(0xFFFDECEE)

private fun parseColor(value: String, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(fallback)

/** Converte a cor principal/secundaria da loja (hex) para Compose, com fallback seguro. */
fun brandColor(hex: String?, fallback: Color = Color(0xFFD9480F)): Color =
    if (hex.isNullOrBlank()) fallback else parseColor(hex, fallback)

@Composable
fun ClienteTheme(
    primaryHex: String = "#D9480F",
    secondaryHex: String = "#B83A0A",
    content: @Composable () -> Unit,
) {
    val primary = parseColor(primaryHex, Color(0xFFD9480F))
    val secondary = parseColor(secondaryHex, primary)
    val scheme = lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.12f),
        onPrimaryContainer = secondary,
        secondary = secondary,
        background = Canvas,
        onBackground = Ink,
        surface = Color.White,
        onSurface = Ink,
        surfaceVariant = Soft,
        onSurfaceVariant = Muted,
        error = Danger,
    )
    val typography = Typography().let { defaults ->
        defaults.copy(
            headlineSmall = defaults.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Ink),
            titleLarge = defaults.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink),
            titleMedium = defaults.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink),
            labelLarge = defaults.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            bodyMedium = defaults.bodyMedium.copy(color = Ink, fontSize = 14.sp),
            bodySmall = defaults.bodySmall.copy(color = Muted, fontSize = 12.sp),
        )
    }
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
