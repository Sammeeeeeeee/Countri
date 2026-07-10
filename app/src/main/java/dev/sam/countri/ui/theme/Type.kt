package dev.sam.countri.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.sam.countri.R

/**
 * The Revolut rule: display never goes heavier than 500 — authority comes
 * from size and tightening tracking, not weight. Inter carries everything
 * (it is the reference's own substitute for Aeonik Pro and its UI face):
 * 500 for display and headings, 400 for body, 600 for button labels, and
 * the only positive tracking in the system on 12sp uppercase captions.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    resId = R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Inter = FontFamily(
    inter(400),
    inter(500),
    inter(600),
)

val DisplayFamily = Inter
val BodyFamily = Inter
val MonoFamily = Inter

object CountriType {
    val hero = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 56.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.024).em,
    )
    val displayLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 46.sp,
        lineHeight = 47.sp,
        letterSpacing = (-0.018).em,
    )
    val display = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 38.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.014).em,
    )
    val displaySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.012).em,
    )
    val title = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em,
    )
    val subtitle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.em,
    )
    val body = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.em,
    )
    val bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.005).em,
    )
    /** Quiet editorial line — weight 400, slate-toned by callers. */
    val quote = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 20.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.005).em,
    )
    /** Uppercase 12sp caption — the system's only positive tracking. */
    val sectionLabel = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.015.em,
    )
    /** Years, metadata, small technical text. */
    val mono = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.em,
    )
    val monoSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.01.em,
    )
}
