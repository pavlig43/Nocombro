package ru.pavlig43.itemlist.internal.ui.settings

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ru.pavlig43.itemlist.internal.ui.ActionIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsToolTip(
    tooltipText: String,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    badge: @Composable BoxScope.() -> Unit = {}
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                caretSize = DpSize(32.dp, 16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 4.dp,
                tonalElevation = 12.dp,
                shape = MaterialTheme.shapes.large,
                content = { Text(tooltipText) }
            )
        },
        state = rememberTooltipState(),
        enableUserInput = true,
        modifier = modifier
    ) {
        ActionIconButton(
            icon = icon,
            onClick = onClick,
            badge = badge
        )
    }
}
