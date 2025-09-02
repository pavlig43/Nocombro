package ru.pavlig43.coreui.tooltip

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import ru.pavlig43.coreui.ActionIconButton

@Composable
fun IconButtonToolTip(
    tooltipText: String,
    onClick: () -> Unit,
    icon: ImageVector,
    tint: Color = LocalContentColor.current,
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
            tint = tint,
            badge = badge
        )
    }
}
