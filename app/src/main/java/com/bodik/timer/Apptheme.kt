package com.bodik.timer

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

data class AppTheme(
    val id: String,
    val label: String,
    val lightColors: ColorScheme,
    val darkColors: ColorScheme,
    val timerTextColor: Color? = null,  // null = использовать onSurface темы
    val accentColor: Color? = null,     // null = использовать primary темы
    val labelColor: Color? = null,      // null = использовать primary.copy(alpha=0.5f)
)

// ─── Color palettes ───────────────────────────────────────────────────────────

private object Palette {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Purple80 = Color(0xFFD0BCFF)

    // Fire — оранжевый фон, чёрный акцент, белые лейблы
    val FireBg = Color(0xFFFF4500)
    val FireTrack = Color(0x33000000)  // чёрный 20% — подложка дуги

    // Neon — почти чёрный фон, лимонный акцент
    val NeonBg = Color(0xFF08080A)
    val NeonAccent = Color(0xFFE3E535)
    val NeonTrack = Color(0x33E3E535)

    // Taxi
    val TaxiBg = Color(0xFFFEED01)
    val TaxiTrack = Color(0x33000000)  // чёрный 20% — подложка дуги

    val DHLBg = Color(0xFFFFCC00)
    val DHLAccent = Color(0xFFD40511)
    val DHLTrack = Color(0x22D40511)

    // Watch
    val WatchBg = Color(0xFFD5D5D2)
    val WatchText = Color(0xFF171C1F)
    val WatchAccent = Color(0xFFDF5B49)
    val WatchTrack = Color(0x33DF5B49)
    val WatchSheet = Color(0xFFE8E8E5)  // чуть светлее фона для bottom sheet

    // Mint
    val MintBg = Color(0xFF31E9E6)
    val MintTrack = Color(0x33000000)

    // Rose
    val RoseBg = Color(0xFFFD6D8B)
    val RoseTrack = Color(0x33000000)

    val LemonBg = Color(0xFFEEF3CC)
    val LemonTrack = Color(0x22000000)
}

// ─── Theme list ───────────────────────────────────────────────────────────────
val AppThemes: List<AppTheme> = listOf(

    AppTheme(
        id = "default",
        label = "Default",
        lightColors = lightColorScheme(
            primary = Color(0xFF4E5E8B)
        ),
        darkColors = darkColorScheme(primary = Palette.Purple80),
    ),

    AppTheme(
        id = "neon",
        label = "Neon",
        timerTextColor = Palette.NeonAccent,
        accentColor = Palette.NeonAccent,
        labelColor = Palette.NeonAccent,
        lightColors = darkColorScheme(
            primary = Palette.NeonAccent,
            onPrimary = Palette.NeonBg,
            error = Palette.NeonAccent,
            background = Palette.NeonBg,
            surface = Palette.NeonBg,
            surfaceVariant = Palette.NeonTrack,
            surfaceContainer = Palette.NeonBg,
            surfaceContainerLow = Palette.NeonBg,
            surfaceContainerHigh = Palette.NeonBg,
            onBackground = Palette.NeonAccent,
            onSurface = Palette.NeonAccent,
        ),
        darkColors = darkColorScheme(
            primary = Palette.NeonAccent,
            onPrimary = Palette.NeonBg,
            error = Palette.NeonAccent,
            background = Palette.NeonBg,
            surface = Palette.NeonBg,
            surfaceVariant = Palette.NeonTrack,
            surfaceContainer = Palette.NeonBg,
            surfaceContainerLow = Palette.NeonBg,
            surfaceContainerHigh = Palette.NeonBg,
            onBackground = Palette.NeonAccent,
            onSurface = Palette.NeonAccent,
        ),
    ),

    AppTheme(
        id = "taxi",
        label = "Taxi",
        timerTextColor = Palette.Black,
        accentColor = Palette.Black,
        labelColor = Palette.Black,
        lightColors = lightColorScheme(
            primary = Palette.Black,
            onPrimary = Palette.White,
            error = Palette.Black,
            background = Palette.TaxiBg,
            surface = Palette.TaxiBg,
            surfaceVariant = Palette.TaxiTrack,
            surfaceContainer = Palette.TaxiBg,
            surfaceContainerLow = Palette.TaxiBg,
            surfaceContainerHigh = Palette.TaxiBg,
            onBackground = Palette.Black,
            onSurface = Palette.Black,
        ),
        darkColors = lightColorScheme(
            primary = Palette.Black,
            onPrimary = Palette.White,
            error = Palette.Black,
            background = Palette.TaxiBg,
            surface = Palette.TaxiBg,
            surfaceVariant = Palette.TaxiTrack,
            surfaceContainer = Palette.TaxiBg,
            surfaceContainerLow = Palette.TaxiBg,
            surfaceContainerHigh = Palette.TaxiBg,
            onBackground = Palette.Black,
            onSurface = Palette.Black,
        ),
    ),

    AppTheme(
        id = "dhl",
        label = "DHL",
        timerTextColor = Palette.DHLAccent,
        accentColor = Palette.DHLAccent,
        labelColor = Palette.DHLAccent,
        lightColors = lightColorScheme(
            primary = Palette.DHLAccent,
            onPrimary = Palette.White,
            error = Palette.DHLAccent,
            background = Palette.DHLBg,
            surface = Palette.DHLBg,
            surfaceVariant = Palette.DHLTrack,
            surfaceContainer = Palette.DHLBg,
            surfaceContainerLow = Palette.DHLBg,
            surfaceContainerHigh = Palette.DHLBg,
            onBackground = Palette.DHLAccent,
            onSurface = Palette.DHLAccent,
        ),
        darkColors = lightColorScheme(
            primary = Palette.DHLAccent,
            onPrimary = Palette.White,
            error = Palette.DHLAccent,
            background = Palette.DHLBg,
            surface = Palette.DHLBg,
            surfaceVariant = Palette.DHLTrack,
            surfaceContainer = Palette.DHLBg,
            surfaceContainerLow = Palette.DHLBg,
            surfaceContainerHigh = Palette.DHLBg,
            onBackground = Palette.DHLAccent,
            onSurface = Palette.DHLAccent,
        ),
    ),

    AppTheme(
        id = "lemon",
        label = "Lemon",
        timerTextColor = Palette.Black,
        accentColor = Palette.Black,
        lightColors = lightColorScheme(
            primary = Palette.Black,
            onPrimary = Palette.LemonBg,
            error = Palette.Black,
            background = Palette.LemonBg,
            surface = Palette.LemonBg,
            surfaceVariant = Palette.LemonTrack,
            surfaceContainer = Palette.LemonBg,
            surfaceContainerLow = Palette.LemonBg,
            surfaceContainerHigh = Palette.LemonBg,
            onBackground = Palette.Black,
            onSurface = Palette.Black,
        ),
        darkColors = lightColorScheme(
            primary = Palette.Black,
            onPrimary = Palette.LemonBg,
            error = Palette.Black,
            background = Palette.LemonBg,
            surface = Palette.LemonBg,
            surfaceVariant = Palette.LemonTrack,
            surfaceContainer = Palette.LemonBg,
            surfaceContainerLow = Palette.LemonBg,
            surfaceContainerHigh = Palette.LemonBg,
            onBackground = Palette.Black,
            onSurface = Palette.Black,
        ),
    ),

    AppTheme(
        id = "fire",
        label = "Fire",
        timerTextColor = Palette.Black,
        accentColor = Palette.Black,
        labelColor = Palette.Black,
        lightColors = lightColorScheme(
            primary = Palette.Black,
            onPrimary = Palette.White,
            error = Palette.Black,
            background = Palette.FireBg,
            surface = Palette.FireBg,
            surfaceVariant = Palette.FireTrack,
            surfaceContainer = Palette.FireBg,
            surfaceContainerLow = Palette.FireBg,
            surfaceContainerHigh = Palette.FireBg,
            onBackground = Palette.Black,
            onSurface = Palette.Black,
        ),
        darkColors = lightColorScheme(
            primary = Palette.Black,
            onPrimary = Palette.White,
            error = Palette.Black,
            background = Palette.FireBg,
            surface = Palette.FireBg,
            surfaceVariant = Palette.FireTrack,
            surfaceContainer = Palette.FireBg,
            surfaceContainerLow = Palette.FireBg,
            surfaceContainerHigh = Palette.FireBg,
            onBackground = Palette.Black,
            onSurface = Palette.Black,
        ),
    ),

    AppTheme(
        id = "watch",
        label = "Watch",
        timerTextColor = Palette.WatchText,
        accentColor = Palette.WatchAccent,
        labelColor = Palette.WatchAccent,
        lightColors = lightColorScheme(
            primary = Palette.WatchAccent,
            onPrimary = Palette.WatchBg,
            error = Palette.WatchAccent,
            background = Palette.WatchBg,
            surface = Palette.WatchBg,
            surfaceVariant = Palette.WatchTrack,
            surfaceContainer = Palette.WatchSheet,
            surfaceContainerLow = Palette.WatchSheet,
            surfaceContainerHigh = Palette.WatchSheet,
            onBackground = Palette.WatchText,
            onSurface = Palette.WatchText,
        ),
        darkColors = lightColorScheme(
            primary = Palette.WatchAccent,
            onPrimary = Palette.WatchBg,
            error = Palette.WatchAccent,
            background = Palette.WatchBg,
            surface = Palette.WatchBg,
            surfaceVariant = Palette.WatchTrack,
            surfaceContainer = Palette.WatchSheet,
            surfaceContainerLow = Palette.WatchSheet,
            surfaceContainerHigh = Palette.WatchSheet,
            onBackground = Palette.WatchText,
            onSurface = Palette.WatchText,
        ),
    ),

    AppTheme(
        id = "mint",
        label = "Mint",
        timerTextColor = Palette.Black,
        accentColor = Palette.Black,
        lightColors = lightColorScheme(
            primary = Palette.Black,
            onPrimary = Palette.MintBg,
            error = Palette.Black,
            background = Palette.MintBg,
            surface = Palette.MintBg,
            surfaceVariant = Palette.MintTrack,
            surfaceContainer = Palette.MintBg,
            surfaceContainerLow = Palette.MintBg,
            surfaceContainerHigh = Palette.MintBg,
            onBackground = Palette.Black,
            onSurface = Palette.Black,
        ),
        darkColors = lightColorScheme(
            primary = Palette.Black,
            onPrimary = Palette.MintBg,
            error = Palette.Black,
            background = Palette.MintBg,
            surface = Palette.MintBg,
            surfaceVariant = Palette.MintTrack,
            surfaceContainer = Palette.MintBg,
            surfaceContainerLow = Palette.MintBg,
            surfaceContainerHigh = Palette.MintBg,
            onBackground = Palette.Black,
            onSurface = Palette.Black,
        ),
    ),

    AppTheme(
        id = "rose",
        label = "Rose",
        timerTextColor = Palette.Black,
        accentColor = Palette.Black,
        lightColors = lightColorScheme(
            primary = Palette.Black,
            onPrimary = Palette.RoseBg,
            error = Palette.Black,
            background = Palette.RoseBg,
            surface = Palette.RoseBg,
            surfaceVariant = Palette.RoseTrack,
            surfaceContainer = Palette.RoseBg,
            surfaceContainerLow = Palette.RoseBg,
            surfaceContainerHigh = Palette.RoseBg,
            onBackground = Palette.Black,
            onSurface = Palette.Black,
        ),
        darkColors = lightColorScheme(
            primary = Palette.Black,
            onPrimary = Palette.RoseBg,
            error = Palette.Black,
            background = Palette.RoseBg,
            surface = Palette.RoseBg,
            surfaceVariant = Palette.RoseTrack,
            surfaceContainer = Palette.RoseBg,
            surfaceContainerLow = Palette.RoseBg,
            surfaceContainerHigh = Palette.RoseBg,
            onBackground = Palette.Black,
            onSurface = Palette.Black,
        ),
    ),
)

fun themeById(id: String): AppTheme = AppThemes.find { it.id == id } ?: AppThemes.first()

// ШРИФТЫ — вынесены отдельно от тем
private val FontDefault = FontFamily(
    Font(R.font.font_regular, FontWeight.Normal),
    Font(R.font.font_bold, FontWeight.Bold)
)

private val DotsFontFamily = FontFamily(
    Font(R.font.dots, FontWeight.Normal),
)

private val OswaldFontFamily = FontFamily(
    Font(R.font.oswald_regular, FontWeight.Normal),
    Font(R.font.oswald_medium, FontWeight.Bold)
)

data class FontOption(
    val id: String,
    val label: String,
    val fontFamily: FontFamily
)

val AvailableFonts: List<FontOption> = listOf(
    FontOption("default", "Default", FontDefault),
    FontOption("oswald", "Oswald", OswaldFontFamily),
    FontOption("dots", "Dots", DotsFontFamily),
)

fun fontById(id: String): FontOption = AvailableFonts.find { it.id == id } ?: AvailableFonts.first()