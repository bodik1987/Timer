package com.bodik.timer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.bodik.timer.R

data class AppTheme(
    val id: String,
    val label: String,
    val lightColors: ColorScheme,
    val darkColors: ColorScheme,
    val timerTextColor: Color? = null,
    val accentColor: Color? = null,
    val labelColor: Color? = null,
)

// ─── Color palettes ───────────────────────────────────────────────────────────

private object Palette {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Purple80 = Color(0xFFD0BCFF)

    val FireBg = Color(0xFFFF4500)
    val FireTrack = Color(0x33000000)

    val NeonBg = Color(0xFF08080A)
    val NeonAccent = Color(0xFFE3E535)
    val NeonTrack = Color(0x33E3E535)

    val NeonGreenAccent = Color(0xFF00FF08)

    val TaxiBg = Color(0xFFFEED01)
    val TaxiTrack = Color(0x33000000)

    val DHLBg = Color(0xFFFFCC00)
    val DHLAccent = Color(0xFFD40511)
    val DHLTrack = Color(0x22D40511)

    val WatchBg = Color(0xFFD5D5D2)
    val WatchText = Color(0xFF171C1F)
    val WatchAccent = Color(0xFFDF5B49)
    val WatchTrack = Color(0x33DF5B49)
    val WatchSheet = Color(0xFFE8E8E5)

    val MintBg = Color(0xFF31E9E6)
    val MintTrack = Color(0x33000000)

    val RoseBg = Color(0xFFFD6D8B)
    val RoseTrack = Color(0x33000000)

    val LemonBg = Color(0xFFEEF3CC)
    val LemonTrack = Color(0x22000000)
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Светлая тема с однотонным фоном: текст и акценты — [fg], фон — [bg], трек — [track].
 */
private fun solidLightScheme(
    fg: Color,
    bg: Color,
    track: Color,
    onPrimary: Color = bg,
) = lightColorScheme(
    primary = fg,
    onPrimary = onPrimary,
    error = fg,
    background = bg,
    surface = bg,
    surfaceVariant = track,
    surfaceContainer = bg,
    surfaceContainerLow = bg,
    surfaceContainerHigh = bg,
    onBackground = fg,
    onSurface = fg,
)

/**
 * Тёмная тема с однотонным фоном (аналог [solidLightScheme] через darkColorScheme).
 */
private fun solidDarkScheme(
    fg: Color,
    bg: Color,
    track: Color,
    onPrimary: Color = bg,
) = darkColorScheme(
    primary = fg,
    onPrimary = onPrimary,
    error = fg,
    background = bg,
    surface = bg,
    surfaceVariant = track,
    surfaceContainer = bg,
    surfaceContainerLow = bg,
    surfaceContainerHigh = bg,
    onBackground = fg,
    onSurface = fg,
)

/**
 * Тема, где light == dark (большинство кастомных тем).
 */
private fun fixedTheme(
    id: String,
    label: String,
    colors: ColorScheme,
    timerTextColor: Color? = null,
    accentColor: Color? = null,
    labelColor: Color? = null,
) = AppTheme(
    id = id,
    label = label,
    lightColors = colors,
    darkColors = colors,
    timerTextColor = timerTextColor,
    accentColor = accentColor,
    labelColor = labelColor,
)

// ─── Theme list ───────────────────────────────────────────────────────────────

val AppThemes: List<AppTheme> = listOf(

    AppTheme(
        id = "default",
        label = "Default",
        lightColors = lightColorScheme(primary = Color(0xFF4E5E8B)),
        darkColors = darkColorScheme(primary = Palette.Purple80),
    ),

    // Neon — тёмная по умолчанию, light == dark
    fixedTheme(
        id = "neon",
        label = "Neon",
        timerTextColor = Palette.NeonAccent,
        accentColor = Palette.NeonAccent,
        labelColor = Palette.NeonAccent,
        colors = solidDarkScheme(Palette.NeonAccent, Palette.NeonBg, Palette.NeonTrack),
    ),

    fixedTheme(
        id = "neonGreen",
        label = "Neon Green",
        timerTextColor = Palette.NeonGreenAccent,
        accentColor = Palette.NeonGreenAccent,
        labelColor = Palette.NeonGreenAccent,
        colors = solidDarkScheme(
            fg = Palette.NeonGreenAccent,
            bg = Palette.Black,
            track = Palette.NeonTrack,
            onPrimary = Palette.Black,
        ),
    ),

    fixedTheme(
        id = "taxi",
        label = "Taxi",
        timerTextColor = Palette.Black,
        accentColor = Palette.Black,
        labelColor = Palette.Black,
        colors = solidLightScheme(
            fg = Palette.Black,
            bg = Palette.TaxiBg,
            track = Palette.TaxiTrack,
            onPrimary = Palette.White,
        ),
    ),

    fixedTheme(
        id = "dhl",
        label = "DHL",
        timerTextColor = Palette.DHLAccent,
        accentColor = Palette.DHLAccent,
        labelColor = Palette.DHLAccent,
        colors = solidLightScheme(
            fg = Palette.DHLAccent,
            bg = Palette.DHLBg,
            track = Palette.DHLTrack,
            onPrimary = Palette.White,
        ),
    ),

    fixedTheme(
        id = "lemon",
        label = "Lemon",
        timerTextColor = Palette.Black,
        accentColor = Palette.Black,
        colors = solidLightScheme(
            fg = Palette.Black,
            bg = Palette.LemonBg,
            track = Palette.LemonTrack,
            onPrimary = Palette.LemonBg,
        ),
    ),

    fixedTheme(
        id = "fire",
        label = "Fire",
        timerTextColor = Palette.Black,
        accentColor = Palette.Black,
        labelColor = Palette.Black,
        colors = solidLightScheme(
            fg = Palette.Black,
            bg = Palette.FireBg,
            track = Palette.FireTrack,
            onPrimary = Palette.White,
        ),
    ),

    // Watch — отдельный случай: surfaceContainer отличается от background
    fixedTheme(
        id = "watch",
        label = "Watch",
        timerTextColor = Palette.WatchText,
        accentColor = Palette.WatchAccent,
        labelColor = Palette.WatchAccent,
        colors = lightColorScheme(
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

    fixedTheme(
        id = "mint",
        label = "Mint",
        timerTextColor = Palette.Black,
        accentColor = Palette.Black,
        colors = solidLightScheme(Palette.Black, Palette.MintBg, Palette.MintTrack),
    ),

    fixedTheme(
        id = "rose",
        label = "Rose",
        timerTextColor = Palette.Black,
        accentColor = Palette.Black,
        colors = solidLightScheme(Palette.Black, Palette.RoseBg, Palette.RoseTrack),
    ),
)

fun themeById(id: String): AppTheme = AppThemes.find { it.id == id } ?: AppThemes.first()

// ─── Fonts ────────────────────────────────────────────────────────────────────

private val FontDefault = FontFamily(
    Font(R.font.font_regular, FontWeight.Normal),
    Font(R.font.font_bold, FontWeight.Bold),
)

private val RobotoFontDefault = FontFamily(
    Font(R.font.roboto_flex_logo, FontWeight.Normal),
)

private val DotsFontFamily = FontFamily(
    Font(R.font.dots, FontWeight.Normal),
)

private val OswaldFontFamily = FontFamily(
    Font(R.font.oswald_regular, FontWeight.Normal),
    Font(R.font.oswald_medium, FontWeight.Bold),
)

private val MonoFontFamily = FontFamily(
    Font(R.font.mono_regular, FontWeight.Normal),
    Font(R.font.mono_medium, FontWeight.Bold),
)

data class FontOption(
    val id: String,
    val label: String,
    val fontFamily: FontFamily,
)

val AvailableFonts: List<FontOption> = listOf(
    FontOption("default", "Default", FontDefault),
    FontOption("oswald", "Oswald", OswaldFontFamily),
    FontOption("logo", "Logo", RobotoFontDefault),
    FontOption("dots", "Dots", DotsFontFamily),
    FontOption("mono", "Mono", MonoFontFamily),
)

fun fontById(id: String): FontOption = AvailableFonts.find { it.id == id } ?: AvailableFonts.first()