package ru.pavlig43.itemlist.internal.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.coreui.itemlist.SearchTextField
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.api.component.refactoring.BaseFilterComponent
import ru.pavlig43.itemlist.api.component.refactoring.ItemFilter1
import ru.pavlig43.itemlist.internal.ui.CREATE_RECORD

@Suppress("LongParameterList")
@Composable
internal fun DefaultSettingsRow(
    onCreate: () -> Unit,
    fullListSelection: List<ItemType>,
    saveSelection: (List<ItemType>) -> Unit,
    searchText: String,
    onSearchChange: (String) -> Unit,
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
        SearchTextField(
            value = searchText,
            onValueChange = onSearchChange
        )


    }
}

@Suppress("LongParameterList")
@Composable
internal fun SettingsRow(
    onCreate: () -> Unit,
    searchText: String,
    onSearchChange: (String) -> Unit,
    filters: @Composable () -> Unit,
    fullListSelection: List<ItemType>,
    saveSelection: (List<ItemType>) -> Unit,

    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButtonToolTip(
            tooltipText = CREATE_RECORD,
            onClick = onCreate,
            icon = Icons.Filled.AddCircle,
        )
        SearchTextField(
            value = searchText,
            onValueChange = onSearchChange
        )

        filters()
        SelectionLogic(
            fullListSelection = fullListSelection,
            saveSelection = saveSelection,
        )

    }
}

@Composable
fun DocuFilter(
    typeComponent: BaseFilterComponent<ItemFilter1.Type<DocumentType>>
) {
    Column() {
        val a = typeComponent.filterFlow.collectAsState()
//        SelectionLogic(
//            fullListSelection = a.value.value,
//            saveSelection = { typeComponent.onChangeFilter(ItemFilter1.Type(it )) }
//        )
    }
}




