package ru.pavlig43.itemlist.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pavlig43.coreui.SearchTextField
import ru.pavlig43.itemlist.internal.BaseFilterComponent
import ru.pavlig43.itemlist.internal.ItemFilter
import ru.pavlig43.itemlist.internal.component.VendorItemUi
import ru.pavlig43.itemlist.internal.component.VendorListComponent
import ru.pavlig43.itemlist.internal.ui.core.ColumnDefinition
import ru.pavlig43.itemlist.internal.ui.core.ItemListBox
import ru.pavlig43.itemlist.internal.ui.settings.SettingsRow

private val columnDefinition = listOf<ColumnDefinition<VendorItemUi>>(
    ColumnDefinition(
        title = ID,
        width = ID_WIDTH,
        valueProvider = { it.id.toString() }
    ),
    ColumnDefinition(
        title = NAME,
        width = NAME_WIDTH,
        valueProvider = { it.displayName }
    ),
    ColumnDefinition(
        title = COMMENT,
        width = COMMENT_WIDTH,
        valueProvider = { it.comment }
    ),

    )

@Composable
internal fun VendorListScreen(
    component: VendorListComponent,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        SettingsRow(
            onCreate = { component.onCreate() }) {
            VendorsFilter(
                searchTextComponent = component.searchTextComponent,
            )
        }
        ItemListBox(
            listComponent = component.staticItemsBodyComponent,
            columnDefinition = columnDefinition,
        )
    }
}

@Composable
private fun VendorsFilter(
    searchTextComponent: BaseFilterComponent<ItemFilter.SearchText>,
) {
    val text by searchTextComponent.filterFlow.collectAsState()

    SearchTextField(
        value = text.value,
        onValueChange = { searchTextComponent.onChangeFilter(ItemFilter.SearchText(it)) },
    )

}