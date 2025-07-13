package ru.pavlig43.coreui.tooltip

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import ru.pavlig43.coreui.ActionIconButton

@Composable
fun IconButtonToolTip(
    tooltipText: String,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    badge: @Composable BoxScope.() -> Unit = {}
) {
    ProjectToolTip(
        tooltipText = tooltipText,
        modifier = modifier
    ) {
        ActionIconButton(
            icon = icon,
            onClick = onClick,
            badge = badge
        )
    }
}
