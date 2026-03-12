package ru.pavlig43.tablecore.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.add_circle

val createButtonNew: @Composable (onCreate: () -> Unit) -> String = { onCreate ->
    ToolTipIconButton(
        "Добавить",
        onClick = onCreate,
        icon = Res.drawable.add_circle,
        tint = MaterialTheme.colorScheme.primary
    )
    ""
}

