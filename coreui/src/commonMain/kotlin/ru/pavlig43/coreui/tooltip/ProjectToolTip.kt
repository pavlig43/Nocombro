package ru.pavlig43.coreui.tooltip

import androidx.compose.material3.*
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectToolTip(
    tooltipText: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TooltipBox(
        positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above, 0.dp),

        tooltip = {
            PlainTooltip(
                caretShape = TooltipDefaults.caretShape(),
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
        modifier = modifier,
        content = content
    )

}