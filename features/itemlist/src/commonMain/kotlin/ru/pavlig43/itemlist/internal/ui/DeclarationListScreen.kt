package ru.pavlig43.itemlist.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pavlig43.core.DateFieldKind
import ru.pavlig43.core.convertToDateOrDateTimeString


import ru.pavlig43.coreui.SearchTextField
import ru.pavlig43.itemlist.internal.BaseFilterComponent
import ru.pavlig43.itemlist.internal.ItemFilter
import ru.pavlig43.itemlist.internal.component.DeclarationItemUi
import ru.pavlig43.itemlist.internal.component.DeclarationListComponent
import ru.pavlig43.itemlist.internal.ui.core.ColumnDefinition
import ru.pavlig43.itemlist.internal.ui.core.ColumnDefinition1
import ru.pavlig43.itemlist.internal.ui.core.ItemListBox1
import ru.pavlig43.itemlist.internal.ui.core.TextCellElement
import ru.pavlig43.itemlist.internal.ui.settings.SettingsRow


private const val BEST_BEFORE_AT = "Годен до"
private val headerCell = listOf(
    TextCellElement(
        highlightedText = ID,
        columnWith = ID_WIDTH,
    ),
    TextCellElement(
        highlightedText = NAME,
        columnWith = NAME_WIDTH,
    ),
    TextCellElement(
        highlightedText = "Поставщик",
        columnWith = NAME_WIDTH,
    ),

    TextCellElement(
        highlightedText = CREATED_AT,
        columnWith = CREATED_AT_WIDTH,
    ),
    TextCellElement(
        highlightedText = BEST_BEFORE_AT,
        columnWith = CREATED_AT_WIDTH,
    ),
)
private val declarationColumns = listOf<ColumnDefinition1<DeclarationItemUi>>(
    ColumnDefinition1(
        width = ID_WIDTH,
        headerTitle = ID,
        cellProvider = { TextCellElement(it.id.toString(), ID_WIDTH) }
    ),
    ColumnDefinition1(
        width = NAME_WIDTH,
        headerTitle = NAME,
        cellProvider = { TextCellElement(it.displayName, NAME_WIDTH) }
    ),
    ColumnDefinition1(
        width = NAME_WIDTH,
        headerTitle = "Поставщик",
        cellProvider = { TextCellElement(it.vendorName, NAME_WIDTH) }
    ),
    ColumnDefinition1(
        width = CREATED_AT_WIDTH,
        headerTitle = CREATED_AT,
        cellProvider = {
            TextCellElement(
                it.createdAt.convertToDateOrDateTimeString(DateFieldKind.Date),
                CREATED_AT_WIDTH
            )
        }
    ),
    ColumnDefinition1(
        width = CREATED_AT_WIDTH,
        headerTitle = BEST_BEFORE_AT,
        cellProvider = {
            TextCellElement(
                it.bestBefore.convertToDateOrDateTimeString(DateFieldKind.Date),
                CREATED_AT_WIDTH
            )
        }
    ),
)


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
        valueProvider = { it.createdAt.convertToDateOrDateTimeString(DateFieldKind.Date) }
    ),
    ColumnDefinition(
        title = BEST_BEFORE_AT,
        width = CREATED_AT_WIDTH,
        valueProvider = { it.bestBefore.convertToDateOrDateTimeString(DateFieldKind.Date) }
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
//        ItemListBox(
//            listComponent = component.staticItemsBodyComponent,
//            columnDefinition = columnDefinition,
//        )
        ItemListBox1(
            listComponent = component.staticItemsBodyComponent1,
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