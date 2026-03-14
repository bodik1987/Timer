package com.bodik.timer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

val LocalFontFamily = staticCompositionLocalOf<FontFamily> {
    error("No font family provided")
}

@Composable
fun TimerTheme(
    appTheme: AppTheme = AppThemes.first(),
    fontFamily: FontFamily,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        appTheme.id == "default" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> appTheme.darkColors
        else -> appTheme.lightColors
    }

    CompositionLocalProvider(LocalFontFamily provides fontFamily) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = timerTypography(fontFamily),
            content = content
        )
    }
}