package ru.pavlig43.coreui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun ActionIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    badge: @Composable BoxScope.() -> Unit = {},

    ) {
    IconButton(onClick, modifier) {
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