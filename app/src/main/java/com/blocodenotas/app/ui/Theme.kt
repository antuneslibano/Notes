package com.blocodenotas.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF6E56CF)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B76E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B2F7A),
    onPrimaryContainer = Color(0xFFE6DFFF),
    secondary = Color(0xFFFFC857),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF4F4F6),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF4F4F6),
    surfaceVariant = Color(0xFF242427),
    onSurfaceVariant = Color(0xFFA6A6AF),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF111113),
    surfaceContainer = Color(0xFF1A1A1D),
    surfaceContainerHigh = Color(0xFF1F1F23),
    surfaceContainerHighest = Color(0xFF29292E),
    outline = Color(0xFF4A4A52),
    outlineVariant = Color(0xFF2E2E33),
    error = Color(0xFFFF8A80),
)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E2FF),
    onPrimaryContainer = Color(0xFF241A5C),
    secondary = Color(0xFFE0A100),
    background = Color(0xFFF6F6F9),
    onBackground = Color(0xFF16161A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16161A),
    surfaceVariant = Color(0xFFECECF1),
    onSurfaceVariant = Color(0xFF6A6A75),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F9FB),
    surfaceContainer = Color(0xFFF3F3F7),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFE6E6EC),
    outline = Color(0xFFBDBDC7),
    outlineVariant = Color(0xFFE2E2E9),
    error = Color(0xFFC62828),
)

@Composable
fun NotesTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
