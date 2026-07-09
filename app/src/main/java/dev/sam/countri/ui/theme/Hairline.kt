package dev.sam.countri.ui.theme

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The only permitted depth cue besides surface steps: a 1dp low-alpha border.
 */
@Composable
fun Modifier.hairline(shape: Shape, color: Color = LocalCountriPalette.current.hairline): Modifier =
    border(1.dp, color, shape)
