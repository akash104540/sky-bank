package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SkyBankDarkColorScheme = darkColorScheme(
    primary = SkyCyan,
    onPrimary = CharcoalDark,
    secondary = TakaGold,
    onSecondary = CharcoalDark,
    tertiary = SkyEmeraldLight,
    onTertiary = Color.White,
    background = CharcoalDark,
    onBackground = IvorySurface,
    surface = Color(0xFF16212C),
    onSurface = IvorySurface,
    surfaceVariant = SkyEmeraldDark,
    onSurfaceVariant = Color(0xFFD1FAE5),
    error = MismatchRed,
    onError = Color.White
)

private val SkyBankLightColorScheme = lightColorScheme(
    primary = SkyEmerald,
    onPrimary = Color.White,
    secondary = TakaGold,
    onSecondary = CharcoalDark,
    tertiary = SkyCyan,
    onTertiary = Color.White,
    background = IvorySurface,
    onBackground = CharcoalDark,
    surface = Color.White,
    onSurface = CharcoalDark,
    surfaceVariant = WarmParchment,
    onSurfaceVariant = SkyEmeraldDark,
    error = MismatchRed,
    onError = Color.White
)

@Composable
fun SkyBankTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SkyBankDarkColorScheme else SkyBankLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
