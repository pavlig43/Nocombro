package ru.pavlig43.coreui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.tooltip.ToolTipIconButton

@Composable
fun NameRowWithSearchIcon(
    text: String,
    onOpenChooseDialog:()-> Unit
){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text, Modifier.weight(1f).padding(start = 4.dp))
        ToolTipIconButton(
            tooltipText = "Выбрать",
            onClick = onOpenChooseDialog,
            icon = Icons.Default.Search,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}