package ru.pavlig43.itemlist.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pavlig43.core.convertToDateTime
import ru.pavlig43.coreui.itemlist.SearchTextField
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.itemlist.internal.BaseFilterComponent
import ru.pavlig43.itemlist.internal.ItemFilter1
import ru.pavlig43.itemlist.internal.component.DocumentItemUi
import ru.pavlig43.itemlist.internal.component.DocumentsListComponent
import ru.pavlig43.itemlist.internal.component.ProductItemUi
import ru.pavlig43.itemlist.internal.component.ProductListComponent
import ru.pavlig43.itemlist.internal.ui.settings.SelectionLogic
import ru.pavlig43.itemlist.internal.ui.settings.SettingsRow

private const val TYPE_PRODUCT = "Тип продукта"
private val columnDefinition = listOf<ColumnDefinition<ProductItemUi>>(
    ColumnDefinition(
        title = ID,
        width = ID_WIDTH,
        valueProvider = {it.id.toString()}
    ),
    ColumnDefinition(
        title = NAME,
        width = NAME_WIDTH,
        valueProvider = {it.displayName}
    ),
    ColumnDefinition(
        title = TYPE_PRODUCT,
        width = TYPE_WIDTH,
        valueProvider = {it.type.displayName}
    ),
    ColumnDefinition(
        title = CREATED_AT,
        width = CREATED_AT_WIDTH,
        valueProvider = {it.createdAt.convertToDateTime()}
    ),
    ColumnDefinition(
        title = COMMENT,
        width = COMMENT_WIDTH,
        valueProvider = {it.comment}
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
        ItemsListBodyScreen(
            listComponent = component.itemsBodyComponent,
            columnDefinition = columnDefinition,
        )
    }
}
@Composable
private fun ProductFilters(
    typeComponent: BaseFilterComponent<ItemFilter1.Type<ProductType>>,
    searchTextComponent: BaseFilterComponent<ItemFilter1.SearchText>,
) {
    val types by typeComponent.filterFlow.collectAsState()
    val text by searchTextComponent.filterFlow.collectAsState()

    SearchTextField(
        value = text.value,
        onValueChange = { searchTextComponent.onChangeFilter(ItemFilter1.SearchText(it)) },
    )
    SelectionLogic(
        fullListSelection = types.value,
        saveSelection = { typeComponent.onChangeFilter(ItemFilter1.Type(it)) }
    )
}