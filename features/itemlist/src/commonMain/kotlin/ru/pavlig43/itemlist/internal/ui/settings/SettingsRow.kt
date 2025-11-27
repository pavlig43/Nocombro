package ru.pavlig43.itemlist.internal.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.itemlist.internal.ui.CREATE_RECORD

@Composable
internal fun SettingsRow(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    filters: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {

        IconButtonToolTip(
            tooltipText = CREATE_RECORD,
            onClick = onCreate,
            icon = Icons.Filled.AddCircle,
        )
        filters()


    }
}