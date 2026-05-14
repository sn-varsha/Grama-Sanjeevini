package com.example.gramasanjeevini.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// ── App-specific dark color scheme (branded teal, not Material default purple) ─
private val AppDarkColorScheme = darkColorScheme(
    primary            = Teal400,
    onPrimary          = Color.White,
    primaryContainer   = Teal900,
    onPrimaryContainer = Teal200,
    secondary          = Teal400,
    background         = Dark900,
    onBackground       = OnDark,
    surface            = Dark800,
    onSurface          = OnDark,
    surfaceVariant     = Dark700,
    onSurfaceVariant   = OnDarkSecondary,
    outline            = Dark600,
    error              = Color(0xFFEF4444),
    onError            = Color.White
)

private val AppLightColorScheme = lightColorScheme(
    primary            = Teal500,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFE6F4F1),
    onPrimaryContainer = Teal900,
    secondary          = Teal500,
    background         = Color(0xFFF3F4F6),
    onBackground       = Color(0xFF111827),
    surface            = Color.White,
    onSurface          = Color(0xFF111827),
    surfaceVariant     = Color(0xFFF9FAFB),
    onSurfaceVariant   = Color(0xFF6B7280),
    outline            = Color(0xFFE5E7EB),
    error              = Color(0xFFDC2626),
    onError            = Color.White
)

// ── CompositionLocal so any composable can read isDarkTheme ─────────────────
val LocalIsDarkTheme = compositionLocalOf { false }

@Composable
fun GramaSanjeeviniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}