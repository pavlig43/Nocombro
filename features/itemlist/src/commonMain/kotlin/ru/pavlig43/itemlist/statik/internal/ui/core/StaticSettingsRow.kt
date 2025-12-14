package ru.pavlig43.itemlist.statik.internal.ui.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.pavlig43.coreui.SearchTextField
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.itemlist.core.component.ValueFilterComponent

@Composable
internal fun StaticSettingsRow(
    onCreate: () -> Unit,
    searchTextComponent: ValueFilterComponent<String>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButtonToolTip(
            tooltipText = "Создать запись",
            onClick = onCreate,
            icon = Icons.Filled.AddCircle,
        )
        val text by searchTextComponent.valueFlow.collectAsState()
        SearchTextField(
            value = text,
            onValueChange =  searchTextComponent::onChange,
        )

    }
}