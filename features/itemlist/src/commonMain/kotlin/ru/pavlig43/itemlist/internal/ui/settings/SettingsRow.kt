package ru.pavlig43.itemlist.internal.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.sharp.FilterAltOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.pavlig43.database.data.common.data.ItemType
import ru.pavlig43.itemlist.internal.ui.CREATE_RECORD
import ru.pavlig43.itemlist.internal.ui.SELECTION

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
        SettingsToolTip(
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



