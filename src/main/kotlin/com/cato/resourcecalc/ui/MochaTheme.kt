package com.cato.resourcecalc.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

object MochaColors {
    val Background = Color(0xFF1E1614)
    val SurfaceLowest = Color(0xFF211916)
    val Surface = Color(0xFF2B211E)
    val SurfaceElevated = Color(0xFF382A25)
    val SurfaceHighest = Color(0xFF44332D)
    val Border = Color(0xFF5A443B)
    val TextPrimary = Color(0xFFF2E7DF)
    val TextSecondary = Color(0xFFCDBBB1)
    val Accent = Color(0xFFC98F73)
    val AccentHover = Color(0xFFDEA98E)
    val AccentContainer = Color(0xFF5B3A2E)
    val Success = Color(0xFFA8BEA0)
    val Warning = Color(0xFFD7B477)
    val Error = Color(0xFFD98787)
    val ErrorContainer = Color(0xFF55312F)
}

private val MinecraftFontFamily = FontFamily(
    Font("fonts/Minecraft.otf", weight = FontWeight.Normal),
)

private val MochaTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = MinecraftFontFamily,
        fontSize = 25.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = MinecraftFontFamily,
        fontSize = 19.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = MinecraftFontFamily,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = MinecraftFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = MinecraftFontFamily,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = MinecraftFontFamily,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = MinecraftFontFamily,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = MinecraftFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = MinecraftFontFamily,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

private val MochaColorScheme = darkColorScheme(
    primary = MochaColors.Accent,
    onPrimary = MochaColors.Background,
    primaryContainer = MochaColors.AccentContainer,
    onPrimaryContainer = MochaColors.TextPrimary,
    secondary = MochaColors.AccentHover,
    onSecondary = MochaColors.Background,
    secondaryContainer = MochaColors.SurfaceHighest,
    onSecondaryContainer = MochaColors.TextPrimary,
    background = MochaColors.Background,
    onBackground = MochaColors.TextPrimary,
    surface = MochaColors.Surface,
    onSurface = MochaColors.TextPrimary,
    surfaceVariant = MochaColors.SurfaceElevated,
    onSurfaceVariant = MochaColors.TextSecondary,
    outline = MochaColors.Border,
    outlineVariant = MochaColors.Border,
    surfaceTint = MochaColors.Accent,
    surfaceDim = MochaColors.Background,
    surfaceBright = MochaColors.SurfaceElevated,
    surfaceContainerLowest = MochaColors.SurfaceLowest,
    surfaceContainerLow = MochaColors.Surface,
    surfaceContainer = MochaColors.Surface,
    surfaceContainerHigh = MochaColors.SurfaceElevated,
    surfaceContainerHighest = MochaColors.SurfaceHighest,
    inverseSurface = MochaColors.SurfaceHighest,
    inverseOnSurface = MochaColors.TextPrimary,
    error = MochaColors.Error,
    onError = MochaColors.Background,
    errorContainer = MochaColors.ErrorContainer,
    onErrorContainer = MochaColors.TextPrimary,
)

@Composable
fun CatosResourceCalcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MochaColorScheme,
        typography = MochaTypography,
        content = content,
    )
}
