package ru.pavlig43.coreui.tooltip

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector


@Suppress("LongParameterList")
@Composable
fun ToolTipIconButton(
    tooltipText: String,
    onClick: () -> Unit,
    icon: ImageVector,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
    modifier: Modifier = Modifier,
    badge: @Composable BoxScope.() -> Unit = {}
) {
    ToolTipProject(
        tooltipText = tooltipText,
        modifier = modifier
    ) {
        ActionIconButton(
            icon = icon,
            onClick = onClick,
            enabled = enabled,
            tint = tint,
            badge = badge
        )
    }
}
@Composable
private fun ActionIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    badge: @Composable BoxScope.() -> Unit = {},

    ) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier) {
        BadgedBox(
            badge = badge
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint
            )
        }

    }
}
