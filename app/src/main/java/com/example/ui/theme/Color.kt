package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Semantic Financial Colors
val IncomeGreen = Color(0xFF006C4C)
val IncomeGreenLight = Color(0xFFD1FAE5)
val IncomeGreenDark = Color(0xFF6CDBAC)

val ExpenseRed = Color(0xFFBA1A1A)
val ExpenseRedLight = Color(0xFFFFDAD6)
val ExpenseRedDark = Color(0xFFFFB4AB)

val TransferTeal = Color(0xFF00687A)
val TransferTealLight = Color(0xFFA6EEFF)

val WarningAmber = Color(0xFF825500)
val WarningAmberLight = Color(0xFFFFDDB3)

// Geometric Balance Palette (Classic Indigo)
val GeoPrimary = Color(0xFF6750A4)
val GeoPrimaryDark = Color(0xFFD0BCFF)
val GeoPrimaryContainer = Color(0xFFEADDFF)
val GeoPrimaryContainerDark = Color(0xFF4F378B)
val GeoOnPrimaryContainer = Color(0xFF21005D)
val GeoOnPrimaryContainerDark = Color(0xFFEADDFF)

val GeoSecondary = Color(0xFF625B71)
val GeoSecondaryContainer = Color(0xFFD0BCFF)
val GeoSecondaryContainerLight = Color(0xFFE8DEF8)
val GeoOnSecondaryContainer = Color(0xFF381E72)

val GeoTertiary = Color(0xFF7D5260)
val GeoTertiaryContainer = Color(0xFFFFD8E4)
val GeoOnTertiaryContainer = Color(0xFF31111D)

val GeoBackground = Color(0xFFFDFBFF)
val GeoBackgroundDark = Color(0xFF141218)
val GeoSurface = Color(0xFFFFFFFF)
val GeoSurfaceDark = Color(0xFF1D1B20)
val GeoSurfaceVariant = Color(0xFFF3EDF7)
val GeoSurfaceVariantDark = Color(0xFF49454F)
val GeoOnSurface = Color(0xFF1C1B1F)
val GeoOnSurfaceDark = Color(0xFFE6E1E5)
val GeoOnSurfaceVariant = Color(0xFF49454F)
val GeoOnSurfaceVariantDark = Color(0xFFCAC4D0)
val GeoOutline = Color(0xFFCAC4D0)
val GeoOutlineDark = Color(0xFF938F99)
val GeoOutlineVariant = Color(0xFFE7E0EC)

// Aliases mapped to Geometric theme
val TealPrimary = GeoPrimary
val TealPrimaryDark = GeoPrimaryDark
val TealContainer = GeoPrimaryContainer
val TealContainerDark = GeoPrimaryContainerDark

enum class AppThemeMode(val displayName: String, val description: String) {
    SYSTEM("System Default", "Follows system dark/light mode"),
    LIGHT("Light Mode", "Bright and crisp interface"),
    DARK("Dark Mode", "Comfortable for nighttime and low-light"),
    AMOLED("Pitch Black (AMOLED)", "Pure black background for OLED screens")
}

enum class AppThemePalette(
    val id: String,
    val displayName: String,
    val description: String,
    val previewPrimary: Color,
    val previewSecondary: Color
) {
    INDIGO(
        id = "INDIGO",
        displayName = "Classic Indigo",
        description = "Royal purple & violet Material 3 balanced theme",
        previewPrimary = Color(0xFF6750A4),
        previewSecondary = Color(0xFFEADDFF)
    ),
    EMERALD(
        id = "EMERALD",
        displayName = "Emerald Forest",
        description = "Wealth & money green with fresh mint accents",
        previewPrimary = Color(0xFF006D40),
        previewSecondary = Color(0xFF9CF5C8)
    ),
    SAPPHIRE(
        id = "SAPPHIRE",
        displayName = "Ocean Sapphire",
        description = "Deep navy & azure banking theme",
        previewPrimary = Color(0xFF0B57D0),
        previewSecondary = Color(0xFFD3E3FD)
    ),
    AMBER(
        id = "AMBER",
        displayName = "Sunset Amber",
        description = "Warm golden amber & rich bronze accents",
        previewPrimary = Color(0xFF8B5000),
        previewSecondary = Color(0xFFFFDDB8)
    ),
    NORDIC_TEAL(
        id = "NORDIC_TEAL",
        displayName = "Nordic Cyan",
        description = "Crisp arctic cyan & serene teal tones",
        previewPrimary = Color(0xFF00687A),
        previewSecondary = Color(0xFFA6EEFF)
    ),
    CRIMSON(
        id = "CRIMSON",
        displayName = "Crimson Ruby",
        description = "Bold ruby wine & warm terracotta",
        previewPrimary = Color(0xFF904A42),
        previewSecondary = Color(0xFFFFDAD5)
    ),
    SLATE(
        id = "SLATE",
        displayName = "Charcoal Slate",
        description = "Modern minimalist graphite & neutral monochrome",
        previewPrimary = Color(0xFF475569),
        previewSecondary = Color(0xFFCBD5E1)
    ),
    DYNAMIC(
        id = "DYNAMIC",
        displayName = "Material You Dynamic",
        description = "Adaptive colors generated from device wallpaper",
        previewPrimary = Color(0xFF3B82F6),
        previewSecondary = Color(0xFF93C5FD)
    ),
    STUDIO(
        id = "STUDIO", displayName = "Quiet Studio",
        description = "Neutral surfaces, restrained teal accents and layered dark cards",
        previewPrimary = Color(0xFF24675F), previewSecondary = Color(0xFFD9EDE7)
    )
}



