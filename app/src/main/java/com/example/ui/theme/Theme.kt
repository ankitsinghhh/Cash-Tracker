package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// --- Palette Color Schemes ---

// 1. INDIGO (Classic Purple/Indigo)
private val IndigoLight = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFEF7FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF3EDF7),
    onSurface = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFCAC4D0),
    error = ExpenseRed
)

private val IndigoDark = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF141218),
    surface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFF49454F),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    error = ExpenseRedDark
)

// 2. EMERALD (Emerald Forest / Money Green)
private val EmeraldLight = lightColorScheme(
    primary = Color(0xFF006D40),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF5C8),
    onPrimaryContainer = Color(0xFF002110),
    secondary = Color(0xFF4E6354),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E8D5),
    onSecondaryContainer = Color(0xFF0C1F13),
    tertiary = Color(0xFF3C6472),
    background = Color(0xFFF6FBF5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDBE5DC),
    onSurface = Color(0xFF171D18),
    onSurfaceVariant = Color(0xFF404942),
    outline = Color(0xFF707972),
    error = ExpenseRed
)

private val EmeraldDark = darkColorScheme(
    primary = Color(0xFF80D8AD),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF00522F),
    onPrimaryContainer = Color(0xFF9CF5C8),
    secondary = Color(0xFFB5CCBA),
    onSecondary = Color(0xFF213527),
    secondaryContainer = Color(0xFF374B3D),
    onSecondaryContainer = Color(0xFFD1E8D5),
    tertiary = Color(0xFFA4CDDD),
    background = Color(0xFF0F1511),
    surface = Color(0xFF171D18),
    surfaceVariant = Color(0xFF404942),
    onSurface = Color(0xFFE0E3DE),
    onSurfaceVariant = Color(0xFFBFC9C0),
    outline = Color(0xFF8A938B),
    error = ExpenseRedDark
)

// 3. SAPPHIRE (Ocean Sapphire / Navy Blue)
private val SapphireLight = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF565F71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF131C2B),
    tertiary = Color(0xFF705574),
    background = Color(0xFFF8F9FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurface = Color(0xFF191C20),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    error = ExpenseRed
)

private val SapphireDark = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF002F6E),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283141),
    secondaryContainer = Color(0xFF3E4759),
    onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFDCBCE0),
    background = Color(0xFF10131B),
    surface = Color(0xFF191C20),
    surfaceVariant = Color(0xFF43474E),
    onSurface = Color(0xFFE2E2E9),
    onSurfaceVariant = Color(0xFFC3C7D0),
    outline = Color(0xFF8D9199),
    error = ExpenseRedDark
)

// 4. AMBER (Sunset Amber / Warm Gold)
private val AmberLight = lightColorScheme(
    primary = Color(0xFF8B5000),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB8),
    onPrimaryContainer = Color(0xFF2C1600),
    secondary = Color(0xFF715B41),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDDFBB),
    onSecondaryContainer = Color(0xFF281805),
    tertiary = Color(0xFF53643E),
    background = Color(0xFFFFF8F4),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0E0D0),
    onSurface = Color(0xFF201A15),
    onSurfaceVariant = Color(0xFF504539),
    outline = Color(0xFF827568),
    error = ExpenseRed
)

private val AmberDark = darkColorScheme(
    primary = Color(0xFFFFB870),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF693C00),
    onPrimaryContainer = Color(0xFFFFDDB8),
    secondary = Color(0xFFDFC3A1),
    onSecondary = Color(0xFF3F2D17),
    secondaryContainer = Color(0xFF58442B),
    onSecondaryContainer = Color(0xFFFDDFBB),
    tertiary = Color(0xFFB9CC9E),
    background = Color(0xFF18120C),
    surface = Color(0xFF201A15),
    surfaceVariant = Color(0xFF504539),
    onSurface = Color(0xFFEEE0D5),
    onSurfaceVariant = Color(0xFFD4C4B5),
    outline = Color(0xFF9C8E80),
    error = ExpenseRedDark
)

// 5. NORDIC_TEAL (Nordic Cyan / Mint Teal)
private val NordicTealLight = lightColorScheme(
    primary = Color(0xFF00687A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA6EEFF),
    onPrimaryContainer = Color(0xFF001F26),
    secondary = Color(0xFF4B6268),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE7EE),
    onSecondaryContainer = Color(0xFF051F24),
    tertiary = Color(0xFF565E7E),
    background = Color(0xFFF5FAFB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDBE4E7),
    onSurface = Color(0xFF161D1E),
    onSurfaceVariant = Color(0xFF3F484B),
    outline = Color(0xFF70797B),
    error = ExpenseRed
)

private val NordicTealDark = darkColorScheme(
    primary = Color(0xFF53D7EE),
    onPrimary = Color(0xFF003640),
    primaryContainer = Color(0xFF004E5C),
    onPrimaryContainer = Color(0xFFA6EEFF),
    secondary = Color(0xFFB1CBD1),
    onSecondary = Color(0xFF1D3439),
    secondaryContainer = Color(0xFF334B50),
    onSecondaryContainer = Color(0xFFCDE7EE),
    tertiary = Color(0xFFBEC6EA),
    background = Color(0xFF0E1516),
    surface = Color(0xFF161D1E),
    surfaceVariant = Color(0xFF3F484B),
    onSurface = Color(0xFFDEE3E5),
    onSurfaceVariant = Color(0xFFBFC8CB),
    outline = Color(0xFF899295),
    error = ExpenseRedDark
)

// 6. CRIMSON (Crimson Ruby / Burgundy)
private val CrimsonLight = lightColorScheme(
    primary = Color(0xFF904A42),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF3B0806),
    secondary = Color(0xFF775652),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD5),
    onSecondaryContainer = Color(0xFF2C1512),
    tertiary = Color(0xFF705C2E),
    background = Color(0xFFFFF8F6),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurface = Color(0xFF231918),
    onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857371),
    error = ExpenseRed
)

private val CrimsonDark = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF561E18),
    primaryContainer = Color(0xFF73332D),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFE7BDB7),
    onSecondary = Color(0xFF442926),
    secondaryContainer = Color(0xFF5D3F3B),
    onSecondaryContainer = Color(0xFFFFDAD5),
    tertiary = Color(0xFFDEC48C),
    background = Color(0xFF1A1110),
    surface = Color(0xFF231918),
    surfaceVariant = Color(0xFF534341),
    onSurface = Color(0xFFF1DFDD),
    onSurfaceVariant = Color(0xFFD8C2BF),
    outline = Color(0xFFA08C8A),
    error = ExpenseRedDark
)

// 7. SLATE (Charcoal Slate / Minimalist Monochrome)
private val SlateLight = lightColorScheme(
    primary = Color(0xFF475569),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = Color(0xFF4B5563),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E7EB),
    onSurface = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFF9CA3AF),
    error = ExpenseRed
)

private val SlateDark = darkColorScheme(
    primary = Color(0xFF94A3B8),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF334155),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = Color(0xFFCBD5E1),
    onSecondary = Color(0xFF1E293B),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = Color(0xFF9CA3AF),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF64748B),
    error = ExpenseRedDark
)

private fun ColorScheme.toAmoled(): ColorScheme {
    return this.copy(
        background = Color.Black,
        surface = Color(0xFF050505),
        surfaceVariant = Color(0xFF121212),
        onSurface = Color(0xFFF5F5F5),
        outline = this.outline.copy(alpha = 0.6f)
    )
}

@Composable
fun MyApplicationTheme(
    palette: AppThemePalette = AppThemePalette.INDIGO,
    mode: AppThemeMode = AppThemeMode.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (mode) {
        AppThemeMode.SYSTEM -> darkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }

    val isAmoled = mode == AppThemeMode.AMOLED
    val context = LocalContext.current

    val baseScheme = when {
        palette == AppThemePalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> when (palette) {
            AppThemePalette.INDIGO, AppThemePalette.DYNAMIC -> if (isDark) IndigoDark else IndigoLight
            AppThemePalette.EMERALD -> if (isDark) EmeraldDark else EmeraldLight
            AppThemePalette.SAPPHIRE -> if (isDark) SapphireDark else SapphireLight
            AppThemePalette.AMBER -> if (isDark) AmberDark else AmberLight
            AppThemePalette.NORDIC_TEAL -> if (isDark) NordicTealDark else NordicTealLight
            AppThemePalette.CRIMSON -> if (isDark) CrimsonDark else CrimsonLight
            AppThemePalette.SLATE -> if (isDark) SlateDark else SlateLight
        }
    }

    val finalScheme = if (isAmoled) baseScheme.toAmoled() else baseScheme

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography,
        content = content
    )
}
