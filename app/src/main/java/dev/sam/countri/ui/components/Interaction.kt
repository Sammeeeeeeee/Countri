package dev.sam.countri.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

/**
 * The app's tap surface. Every hand-built control is a plain [androidx.compose.foundation.layout.Box]
 * or [androidx.compose.foundation.layout.Row], so without this they'd reach the accessibility tree
 * as anonymous, unannounced nodes. This bundles the ripple-free click the
 * design calls for with a [Role] and an optional spoken label, so TalkBack
 * announces "button, <label>" the way a real Material control would.
 *
 * [onClickLabel] describes the action (e.g. "Mark visited"); pass it whenever
 * the control's visible text or icon description doesn't already say what a
 * tap does.
 */
fun Modifier.tapTarget(
    role: Role = Role.Button,
    onClickLabel: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    clickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
        role = role,
        onClickLabel = onClickLabel,
        onClick = onClick,
    )
}
