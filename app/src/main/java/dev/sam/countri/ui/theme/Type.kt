package dev.sam.countri.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.sam.countri.R

/**
 * One weight, many sizes. Hierarchy is carved from scale and negative
 * tracking — never from boldness. Mono is the instrument-label voice:
 * ISO codes, section labels, metadata.
 */
val DisplayFamily = FontFamily(
    Font(R.font.inter_tight, FontWeight.Normal),
    Font(R.font.inter_tight_italic, FontWeight.Normal, FontStyle.Italic),
)

val MonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
)

object CountriType {
    val hero = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 76.sp,
        lineHeight = 76.sp,
        letterSpacing = (-0.03).em,
    )
    val displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 58.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.028).em,
    )
    val display = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 46.sp,
        lineHeight = 45.sp,
        letterSpacing = (-0.025).em,
    )
    val displaySmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.02).em,
    )
    val title = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.015).em,
    )
    val subtitle = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.01).em,
    )
    val body = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.005).em,
    )
    val bodySmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.em,
    )
    /** RECENT JOURNEYS · BY CONTINENT — tracked-out mono section labels. */
    val sectionLabel = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.16.em,
    )
    /** ISO codes, years, metadata. */
    val mono = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.em,
    )
    val monoSmall = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.08.em,
    )
}
