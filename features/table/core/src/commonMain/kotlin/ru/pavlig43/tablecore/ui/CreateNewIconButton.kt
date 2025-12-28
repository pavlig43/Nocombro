package ru.pavlig43.tablecore.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import ru.pavlig43.coreui.tooltip.IconButtonToolTip

val createButtonNew: @Composable (onCreate: () -> Unit) -> String = { onCreate ->
    IconButtonToolTip(
        "Добавить",
        onClick = onCreate,
        icon = Icons.Default.AddCircle,
        tint = MaterialTheme.colorScheme.primary
    )
    ""
}

