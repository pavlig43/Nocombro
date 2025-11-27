package ru.pavlig43.itemlist.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pavlig43.core.convertToDate
import ru.pavlig43.core.convertToDateTime
import ru.pavlig43.coreui.SearchTextField
import ru.pavlig43.itemlist.internal.BaseFilterComponent
import ru.pavlig43.itemlist.internal.ItemFilter
import ru.pavlig43.itemlist.internal.component.DeclarationItemUi
import ru.pavlig43.itemlist.internal.component.DeclarationListComponent
import ru.pavlig43.itemlist.internal.ui.core.ColumnDefinition
import ru.pavlig43.itemlist.internal.ui.core.ItemListBox
import ru.pavlig43.itemlist.internal.ui.settings.SettingsRow


private const val BEST_BEFORE_AT = "Годен до"
private val columnDefinition = listOf<ColumnDefinition<DeclarationItemUi>>(
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
        title = "Поставщик",
        width = NAME_WIDTH,
        valueProvider = { it.vendorName }
    ),

    ColumnDefinition(
        title = CREATED_AT,
        width = CREATED_AT_WIDTH,
        valueProvider = { it.createdAt.convertToDateTime() }
    ),
    ColumnDefinition(
        title = BEST_BEFORE_AT,
        width = CREATED_AT_WIDTH,
        valueProvider = { it.bestBefore.convertToDate() }
    ),

)

@Composable
internal fun DeclarationListScreen(
    component: DeclarationListComponent,
    modifier: Modifier = Modifier
){
    Column(modifier.fillMaxWidth()) {
        SettingsRow(
            onCreate = { component.onCreate() }) {
            DeclarationsFilter(
                searchTextComponent = component.searchTextComponent,
            )
        }
        ItemListBox(
            listComponent = component.itemsBodyComponent,
            columnDefinition = columnDefinition,
        )
    }
}
@Composable
private fun DeclarationsFilter(
    searchTextComponent: BaseFilterComponent<ItemFilter.SearchText>,
) {
    val text by searchTextComponent.filterFlow.collectAsState()

    SearchTextField(
        value = text.value,
        onValueChange = { searchTextComponent.onChangeFilter(ItemFilter.SearchText(it)) },
    )

}