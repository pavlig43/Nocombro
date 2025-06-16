package ru.pavlig43.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font

private val fontFamily: FontFamily
    @Composable get() = FontFamily(
        // Black
        Font(Res.font.MontserratAlternates_Black, weight = FontWeight.Black),
        Font(Res.font.MontserratAlternates_BlackItalic, weight = FontWeight.Black, style = FontStyle.Italic),

        // ExtraBold (FontWeight.ExtraBold = 800)
        Font(Res.font.MontserratAlternates_ExtraBold, weight = FontWeight.ExtraBold),
        Font(Res.font.MontserratAlternates_ExtraBoldItalic, weight = FontWeight.ExtraBold, style = FontStyle.Italic),

        // Bold
        Font(Res.font.MontserratAlternates_Bold, weight = FontWeight.Bold),
        Font(Res.font.MontserratAlternates_BoldItalic, weight = FontWeight.Bold, style = FontStyle.Italic),

        // SemiBold (FontWeight.SemiBold = 600)
        Font(Res.font.MontserratAlternates_SemiBold, weight = FontWeight.SemiBold),
        Font(Res.font.MontserratAlternates_SemiBoldItalic, weight = FontWeight.SemiBold, style = FontStyle.Italic),

        // Medium (FontWeight.Medium = 500)
        Font(Res.font.MontserratAlternates_Medium, weight = FontWeight.Medium),
        Font(Res.font.MontserratAlternates_MediumItalic, weight = FontWeight.Medium, style = FontStyle.Italic),

        // Regular/Normal
        Font(Res.font.MontserratAlternates_Regular, weight = FontWeight.Normal),
        Font(Res.font.MontserratAlternates_Italic, weight = FontWeight.Normal, style = FontStyle.Italic),

        // Light
        Font(Res.font.MontserratAlternates_Light, weight = FontWeight.Light),
        Font(Res.font.MontserratAlternates_LightItalic, weight = FontWeight.Light, style = FontStyle.Italic),

        // ExtraLight (FontWeight.ExtraLight = 200)
        Font(Res.font.MontserratAlternates_ExtraLight, weight = FontWeight.ExtraLight),
        Font(Res.font.MontserratAlternates_ExtraLightItalic, weight = FontWeight.ExtraLight, style = FontStyle.Italic),

        // Thin
        Font(Res.font.MontserratAlternates_Thin, weight = FontWeight.Thin),
        Font(Res.font.MontserratAlternates_ThinItalic, weight = FontWeight.Thin, style = FontStyle.Italic)
    )

internal val NocombroTypography: Typography
    @Composable get() {
        val baseline = Typography()

        return Typography(
            displayLarge = baseline.displayLarge.copy(fontFamily = fontFamily),
            displayMedium = baseline.displayMedium.copy(fontFamily = fontFamily),
            displaySmall = baseline.displaySmall.copy(fontFamily = fontFamily),
            headlineLarge = baseline.headlineLarge.copy(fontFamily = fontFamily),
            headlineMedium = baseline.headlineMedium.copy(fontFamily = fontFamily),
            headlineSmall = baseline.headlineSmall.copy(fontFamily = fontFamily),
            titleLarge = baseline.titleLarge.copy(fontFamily = fontFamily),
            titleMedium = baseline.titleMedium.copy(fontFamily = fontFamily),
            titleSmall = baseline.titleSmall.copy(fontFamily = fontFamily),
            bodyLarge = baseline.bodyLarge.copy(fontFamily = fontFamily),
            bodyMedium = baseline.bodyMedium.copy(fontFamily = fontFamily),
            bodySmall = baseline.bodySmall.copy(fontFamily = fontFamily),
            labelLarge = baseline.labelLarge.copy(fontFamily = fontFamily),
            labelMedium = baseline.labelMedium.copy(fontFamily = fontFamily),
            labelSmall = baseline.labelSmall.copy(fontFamily = fontFamily),
        )
    }
