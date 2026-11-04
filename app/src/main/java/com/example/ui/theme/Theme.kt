package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.example.data.preferences.AppearanceMode
import com.example.data.preferences.DesignTheme

val LocalIsDarkTheme = compositionLocalOf { false }
val LocalIsAmoled = compositionLocalOf { false }
val LocalDesignTheme = compositionLocalOf { DesignTheme.MATERIAL_3 }

// Dark Mode with Ash Background (User requested Ash as dark mode background)
private val AshDarkColorScheme = darkColorScheme(
    primary = LavenderAccent,
    onPrimary = AshBackground,
    primaryContainer = DarkLavenderCard,
    onPrimaryContainer = PastelLavender,
    secondary = PowderBlueAccent,
    onSecondary = AshBackground,
    secondaryContainer = DarkBlueCard,
    onSecondaryContainer = PastelPowderBlue,
    tertiary = MintAccent,
    onTertiary = AshBackground,
    tertiaryContainer = DarkMintCard,
    onTertiaryContainer = PastelMint,
    background = AshBackground,
    onBackground = AshText,
    surface = AshSurface,
    onSurface = AshText,
    surfaceVariant = AshCard,
    onSurfaceVariant = AshSubtext,
    outline = AshDivider
)

// AMOLED True Black Mode (User requested AMOLED option when dark mode is active)
private val AmoledDarkColorScheme = darkColorScheme(
    primary = LavenderAccent,
    onPrimary = AmoledBackground,
    primaryContainer = DarkLavenderCard,
    onPrimaryContainer = PastelLavender,
    secondary = PowderBlueAccent,
    onSecondary = AmoledBackground,
    secondaryContainer = DarkBlueCard,
    onSecondaryContainer = PastelPowderBlue,
    tertiary = MintAccent,
    onTertiary = AmoledBackground,
    tertiaryContainer = DarkMintCard,
    onTertiaryContainer = PastelMint,
    background = AmoledBackground,
    onBackground = AmoledText,
    surface = AmoledSurface,
    onSurface = AmoledText,
    surfaceVariant = AmoledCard,
    onSurfaceVariant = AmoledSubtext,
    outline = AmoledDivider
)

// Light Mode with Cream Background (User requested shade of cream instead of white)
private val LightColorScheme = lightColorScheme(
    primary = LavenderAccent,
    onPrimary = WarmCreamCard,
    primaryContainer = PastelLavenderContainer,
    onPrimaryContainer = LavenderAccent,
    secondary = PowderBlueAccent,
    onSecondary = WarmCreamCard,
    secondaryContainer = PastelPowderBlueContainer,
    onSecondaryContainer = PowderBlueAccent,
    tertiary = MintAccent,
    onTertiary = WarmCreamCard,
    tertiaryContainer = PastelMintContainer,
    onTertiaryContainer = MintAccent,
    background = WarmCreamBackground,
    onBackground = WarmCreamText,
    surface = WarmCreamSurface,
    onSurface = WarmCreamText,
    surfaceVariant = WarmCreamCard,
    onSurfaceVariant = WarmCreamSubtext,
    outline = WarmCreamDivider
)

@Composable
fun MitmatTheme(
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    isAmoled: Boolean = false,
    designTheme: DesignTheme = DesignTheme.MATERIAL_3,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (appearanceMode) {
        AppearanceMode.SYSTEM -> systemInDark
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }

    val activeAmoled = isDark && isAmoled
    val colorScheme = when {
        activeAmoled -> AmoledDarkColorScheme
        isDark -> AshDarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides isDark,
        LocalIsAmoled provides activeAmoled,
        LocalDesignTheme provides designTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
