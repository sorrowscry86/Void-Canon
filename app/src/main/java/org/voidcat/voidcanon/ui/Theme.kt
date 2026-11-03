package org.voidcat.voidcanon.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF101115)
val Panel = Color(0xFF1C1B23)
val Warm = Color(0xFFF2ECE2)
val Muted = Color(0xFFBCB7C6)
val Violet = Color(0xFFC8B0EF)
val Line = Color(0xFF393541)

@Composable
fun VoidCanonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet, onPrimary = Color(0xFF25173E), primaryContainer = Color(0xFF594675),
            onPrimaryContainer = Warm, background = Ink, onBackground = Warm,
            surface = Ink, onSurface = Warm, surfaceVariant = Panel, onSurfaceVariant = Muted,
            outline = Line, secondary = Color(0xFFD4BE95),
        ),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 34.sp, lineHeight = 39.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 34.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 23.sp, lineHeight = 29.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
        ), content = content,
    )
}
