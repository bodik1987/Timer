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
    // Default — purple (Material baseline)
    val Purple80 = Color(0xFFD0BCFF)
    val Purple40 = Color(0xFF6650A4)

    // Acid — кислотный жёлто-зелёный на чёрном
    val AcidGreen = Color(0xFFD9F807)
    val AcidRed = Color(0xFFFF3B30)  // цвет фазы отдыха
    val AcidBg = Color(0xFF000000)
    val AcidSurface = Color(0xFF0D0D0D)
    val AcidOnBg = Color(0xFFFFFFFF)

    // Fire — оранжевый фон, чёрный акцент, белые лейблы
    val FireBg = Color(0xFFFF4500)
    val FirePrimary = Color(0xFF000000)
    val FireLabel = Color(0xFFFFFFFF)
    val FireTrack = Color(0x33000000)  // чёрный 20% — подложка дуги

    // Neon — почти чёрный фон, лимонный акцент
    val NeonBg = Color(0xFF08080A)
    val NeonAccent = Color(0xFFE3E535)
    val NeonTrack = Color(0x33E3E535)  // акцент 20% — подложка дуги

    // Taxi — жёлтый фон, чёрный акцент, белые лейблы
    val TaxiBg = Color(0xFFFEED01)
    val TaxiAccent = Color(0xFF000000)
    val TaxiLabel = Color(0xFFFFFFFF)
    val TaxiTrack = Color(0x33000000)  // чёрный 20% — подложка дуги

    // Watch — светло-серый фон, тёмный текст, красный акцент
    val WatchBg = Color(0xFFD5D5D2)
    val WatchText = Color(0xFF171C1F)
    val WatchAccent = Color(0xFFDF5B49)
    val WatchTrack = Color(0x33DF5B49)
    val WatchSheet = Color(0xFFE8E8E5)  // чуть светлее фона для bottom sheet

    // Cyber — чёрный фон, кислотный циан
    val CyberBg = Color(0xFF0A0A0A)
    val CyberAccent = Color(0xFF00F5D4)
    val CyberTrack = Color(0x2200F5D4)  // циан 13% — подложка дуги

    // Mint — бирюзовый фон, чёрный текст
    val MintBg = Color(0xFF31E9E6)
    val MintText = Color(0xFF000000)
    val MintTrack = Color(0x33000000)

    // Rose — розовый фон, чёрный текст
    val RoseBg = Color(0xFFFD6D8B)
    val RoseText = Color(0xFF000000)
    val RoseTrack = Color(0x33000000)

    val LemonBg = Color(0xFFEEF3CC)
    val LemonText = Color(0xFF000000)
    val LemonTrack = Color(0x22000000)
}

// ─── Theme list ───────────────────────────────────────────────────────────────
// Порядок определяет порядок в селекторе.
// Чтобы добавить тему — допиши AppTheme(...) в этот список. Больше ничего менять не нужно.

val AppThemes: List<AppTheme> = listOf(

    AppTheme(
        id = "default",
        label = "Default",
        lightColors = lightColorScheme(primary = Palette.Purple40),
        darkColors = darkColorScheme(primary = Palette.Purple80),
    ),

    AppTheme(
        id = "acid",
        label = "Acid",
        timerTextColor = Palette.AcidGreen,
        lightColors = darkColorScheme(
            primary = Palette.AcidGreen,
            onPrimary = Palette.AcidBg,
            error = Palette.AcidRed,
            background = Palette.AcidBg,
            surface = Palette.AcidBg,
            surfaceVariant = Palette.AcidSurface,
            surfaceContainer = Palette.AcidSurface,
            surfaceContainerLow = Palette.AcidSurface,
            surfaceContainerHigh = Palette.AcidSurface,
            onBackground = Palette.AcidOnBg,
            onSurface = Palette.AcidOnBg,
        ),
        darkColors = darkColorScheme(
            primary = Palette.AcidGreen,
            onPrimary = Palette.AcidBg,
            error = Palette.AcidRed,
            background = Palette.AcidBg,
            surface = Palette.AcidBg,
            surfaceVariant = Palette.AcidSurface,
            surfaceContainer = Palette.AcidSurface,
            surfaceContainerLow = Palette.AcidSurface,
            surfaceContainerHigh = Palette.AcidSurface,
            onBackground = Palette.AcidOnBg,
            onSurface = Palette.AcidOnBg,
        ),
    ),

    AppTheme(
        id = "fire",
        label = "Fire",
        timerTextColor = Palette.FirePrimary,
        accentColor = Palette.FirePrimary,
        labelColor = Palette.FireLabel,
        lightColors = lightColorScheme(
            primary = Palette.FirePrimary,
            onPrimary = Palette.FireLabel,
            error = Palette.FirePrimary,
            background = Palette.FireBg,
            surface = Palette.FireBg,
            surfaceVariant = Palette.FireTrack,
            surfaceContainer = Palette.FireBg,
            surfaceContainerLow = Palette.FireBg,
            surfaceContainerHigh = Palette.FireBg,
            onBackground = Palette.FirePrimary,
            onSurface = Palette.FirePrimary,
        ),
        darkColors = lightColorScheme(
            primary = Palette.FirePrimary,
            onPrimary = Palette.FireLabel,
            error = Palette.FirePrimary,
            background = Palette.FireBg,
            surface = Palette.FireBg,
            surfaceVariant = Palette.FireTrack,
            surfaceContainer = Palette.FireBg,
            surfaceContainerLow = Palette.FireBg,
            surfaceContainerHigh = Palette.FireBg,
            onBackground = Palette.FirePrimary,
            onSurface = Palette.FirePrimary,
        ),
    ),

    AppTheme(
        id = "neon",
        label = "Neon",
        timerTextColor = Palette.NeonAccent,
        accentColor = Palette.NeonAccent,
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
        timerTextColor = Palette.TaxiAccent,
        accentColor = Palette.TaxiAccent,
        labelColor = Palette.TaxiLabel,
        lightColors = lightColorScheme(
            primary = Palette.TaxiAccent,
            onPrimary = Palette.TaxiLabel,
            error = Palette.TaxiAccent,
            background = Palette.TaxiBg,
            surface = Palette.TaxiBg,
            surfaceVariant = Palette.TaxiTrack,
            surfaceContainer = Palette.TaxiBg,
            surfaceContainerLow = Palette.TaxiBg,
            surfaceContainerHigh = Palette.TaxiBg,
            onBackground = Palette.TaxiAccent,
            onSurface = Palette.TaxiAccent,
        ),
        darkColors = lightColorScheme(
            primary = Palette.TaxiAccent,
            onPrimary = Palette.TaxiLabel,
            error = Palette.TaxiAccent,
            background = Palette.TaxiBg,
            surface = Palette.TaxiBg,
            surfaceVariant = Palette.TaxiTrack,
            surfaceContainer = Palette.TaxiBg,
            surfaceContainerLow = Palette.TaxiBg,
            surfaceContainerHigh = Palette.TaxiBg,
            onBackground = Palette.TaxiAccent,
            onSurface = Palette.TaxiAccent,
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
        timerTextColor = Palette.MintText,
        accentColor = Palette.MintText,
        lightColors = lightColorScheme(
            primary = Palette.MintText,
            onPrimary = Palette.MintBg,
            error = Palette.MintText,
            background = Palette.MintBg,
            surface = Palette.MintBg,
            surfaceVariant = Palette.MintTrack,
            surfaceContainer = Palette.MintBg,
            surfaceContainerLow = Palette.MintBg,
            surfaceContainerHigh = Palette.MintBg,
            onBackground = Palette.MintText,
            onSurface = Palette.MintText,
        ),
        darkColors = lightColorScheme(
            primary = Palette.MintText,
            onPrimary = Palette.MintBg,
            error = Palette.MintText,
            background = Palette.MintBg,
            surface = Palette.MintBg,
            surfaceVariant = Palette.MintTrack,
            surfaceContainer = Palette.MintBg,
            surfaceContainerLow = Palette.MintBg,
            surfaceContainerHigh = Palette.MintBg,
            onBackground = Palette.MintText,
            onSurface = Palette.MintText,
        ),
    ),

    AppTheme(
        id = "rose",
        label = "Rose",
        timerTextColor = Palette.RoseText,
        accentColor = Palette.RoseText,
        lightColors = lightColorScheme(
            primary = Palette.RoseText,
            onPrimary = Palette.RoseBg,
            error = Palette.RoseText,
            background = Palette.RoseBg,
            surface = Palette.RoseBg,
            surfaceVariant = Palette.RoseTrack,
            surfaceContainer = Palette.RoseBg,
            surfaceContainerLow = Palette.RoseBg,
            surfaceContainerHigh = Palette.RoseBg,
            onBackground = Palette.RoseText,
            onSurface = Palette.RoseText,
        ),
        darkColors = lightColorScheme(
            primary = Palette.RoseText,
            onPrimary = Palette.RoseBg,
            error = Palette.RoseText,
            background = Palette.RoseBg,
            surface = Palette.RoseBg,
            surfaceVariant = Palette.RoseTrack,
            surfaceContainer = Palette.RoseBg,
            surfaceContainerLow = Palette.RoseBg,
            surfaceContainerHigh = Palette.RoseBg,
            onBackground = Palette.RoseText,
            onSurface = Palette.RoseText,
        ),
    ),

    AppTheme(
        id = "lemon",
        label = "Lemon",
        timerTextColor = Palette.LemonText,
        accentColor = Palette.LemonText,
        lightColors = lightColorScheme(
            primary = Palette.LemonText,
            onPrimary = Palette.LemonBg,
            error = Palette.LemonText,
            background = Palette.LemonBg,
            surface = Palette.LemonBg,
            surfaceVariant = Palette.LemonTrack,
            surfaceContainer = Palette.LemonBg,
            surfaceContainerLow = Palette.LemonBg,
            surfaceContainerHigh = Palette.LemonBg,
            onBackground = Palette.LemonText,
            onSurface = Palette.LemonText,
        ),
        darkColors = lightColorScheme(
            primary = Palette.LemonText,
            onPrimary = Palette.LemonBg,
            error = Palette.LemonText,
            background = Palette.LemonBg,
            surface = Palette.LemonBg,
            surfaceVariant = Palette.LemonTrack,
            surfaceContainer = Palette.LemonBg,
            surfaceContainerLow = Palette.LemonBg,
            surfaceContainerHigh = Palette.LemonBg,
            onBackground = Palette.LemonText,
            onSurface = Palette.LemonText,
        ),
    ),
)

// Удобный доступ по id (используется при загрузке из DataStore)
fun themeById(id: String): AppTheme = AppThemes.find { it.id == id } ?: AppThemes.first()

// ─────────────────────────────────────────────────────────────────────────────
// ШРИФТЫ — вынесены отдельно от тем
// ─────────────────────────────────────────────────────────────────────────────

private val FontDefault = FontFamily(
    Font(R.font.font_regular, FontWeight.Normal),
    Font(R.font.font_bold, FontWeight.Bold)
)

private val PantonFontFamily = FontFamily(
    Font(R.font.panton_regular, FontWeight.Normal),
    Font(R.font.panton_bold, FontWeight.Bold)
)

data class FontOption(
    val id: String,
    val label: String,
    val fontFamily: FontFamily
)

val AvailableFonts: List<FontOption> = listOf(
    FontOption("default", "Default", FontDefault),
    FontOption("panton", "Panton", PantonFontFamily)
)

fun fontById(id: String): FontOption = AvailableFonts.find { it.id == id } ?: AvailableFonts.first()