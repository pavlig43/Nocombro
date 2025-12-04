package ru.pavlig43.itemlist.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pavlig43.core.convertToDateTime
import ru.pavlig43.coreui.SearchTextField
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.itemlist.internal.BaseFilterComponent
import ru.pavlig43.itemlist.internal.ItemFilter
import ru.pavlig43.itemlist.internal.component.ProductItemUi
import ru.pavlig43.itemlist.internal.component.ProductListComponent
import ru.pavlig43.itemlist.internal.ui.core.ColumnDefinition
import ru.pavlig43.itemlist.internal.ui.core.ItemListBox
import ru.pavlig43.itemlist.internal.ui.settings.SelectionLogic
import ru.pavlig43.itemlist.internal.ui.settings.SettingsRow

private const val TYPE_PRODUCT = "Тип продукта"
private val columnDefinition = listOf<ColumnDefinition<ProductItemUi>>(
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
        title = TYPE_PRODUCT,
        width = TYPE_WIDTH,
        valueProvider = { it.type.displayName }
    ),
    ColumnDefinition(
        title = CREATED_AT,
        width = CREATED_AT_WIDTH,
        valueProvider = { it.createdAt.convertToDateTime() }
    ),
    ColumnDefinition(
        title = COMMENT,
        width = COMMENT_WIDTH,
        valueProvider = { it.comment }
    )
)

@Composable
internal fun ProductListScreen(
    component: ProductListComponent,
    modifier: Modifier = Modifier
){
    Column(modifier.fillMaxWidth()) {
        SettingsRow(
            onCreate = { component.onCreate() }) {
            ProductFilters(
                typeComponent = component.typeComponent,
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
private fun ProductFilters(
    typeComponent: BaseFilterComponent<ItemFilter.Type<ProductType>>,
    searchTextComponent: BaseFilterComponent<ItemFilter.SearchText>,
) {
    val types by typeComponent.filterFlow.collectAsState()
    val text by searchTextComponent.filterFlow.collectAsState()

    SearchTextField(
        value = text.value,
        onValueChange = { searchTextComponent.onChangeFilter(ItemFilter.SearchText(it)) },
    )
    SelectionLogic(
        fullListSelection = types.value,
        saveSelection = { typeComponent.onChangeFilter(ItemFilter.Type(it)) }
    )
}