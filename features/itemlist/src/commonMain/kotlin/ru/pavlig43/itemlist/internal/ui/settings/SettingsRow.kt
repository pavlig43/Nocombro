package ru.pavlig43.itemlist.internal.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.itemlist.internal.ui.CREATE_RECORD
import ru.pavlig43.coreui.tooltip.IconButtonToolTip


@Composable
internal fun SettingsRow(
    onCreate: () -> Unit,
    fullListSelection: List<ItemType>,
    saveSelection: (List<ItemType>) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().height(32.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButtonToolTip(
            tooltipText = CREATE_RECORD,
            onClick = onCreate,
            icon = Icons.Filled.AddCircle,
        )
        SelectionLogic(
            fullListSelection = fullListSelection,
            saveSelection = saveSelection,
        )

    }
}



