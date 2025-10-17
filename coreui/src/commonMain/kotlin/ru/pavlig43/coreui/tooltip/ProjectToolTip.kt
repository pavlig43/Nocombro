package ru.pavlig43.coreui.tooltip

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectToolTip(
    tooltipText: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
){
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
        modifier = modifier,
        content = content
    )

}