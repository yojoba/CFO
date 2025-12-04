package com.example.protfoliomanagerv1.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ProtfolioManagerv1Theme(
    darkTheme: Boolean = true, // Force dark theme for crypto aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = CryptoDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CryptoTypography,
        content = content
    )
}

// Crypto Dark Color Scheme
private val CryptoDarkColorScheme = darkColorScheme(
    primary = GradientPurple,
    onPrimary = TextPrimary,
    primaryContainer = CryptoDarkSurfaceVariant,
    onPrimaryContainer = TextPrimary,
    
    secondary = GradientBlue,
    onSecondary = TextPrimary,
    secondaryContainer = CryptoDarkSurfaceVariant,
    onSecondaryContainer = TextSecondary,
    
    tertiary = NeonGreen,
    onTertiary = CryptoDarkBackground,
    tertiaryContainer = ProfitGreenBg,
    onTertiaryContainer = NeonGreen,
    
    error = ElectricRed,
    onError = TextPrimary,
    errorContainer = LossRedBg,
    onErrorContainer = ElectricRed,
    
    background = CryptoDarkBackground,
    onBackground = TextPrimary,
    
    surface = CryptoDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CryptoDarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    
    outline = ChartGrid,
    outlineVariant = CryptoDarkSurfaceVariant,
    
    inverseSurface = TextPrimary,
    inverseOnSurface = CryptoDarkBackground,
    inversePrimary = GradientPurple,
    
    surfaceTint = GradientBlue,
    scrim = Color(0xFF000000)
)
