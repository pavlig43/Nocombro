package ru.pavlig43.itemlist.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ru.pavlig43.core.DateFieldKind
import ru.pavlig43.core.convertToDateOrDateTimeString
import ru.pavlig43.coreui.SearchTextField
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.itemlist.internal.BaseFilterComponent
import ru.pavlig43.itemlist.internal.ItemFilter
import ru.pavlig43.itemlist.internal.component.TransactionItemUi
import ru.pavlig43.itemlist.internal.component.TransactionListComponent
import ru.pavlig43.itemlist.internal.ui.core.ColumnDefinition
import ru.pavlig43.itemlist.internal.ui.core.ItemListBox
import ru.pavlig43.itemlist.internal.ui.settings.SelectionLogic
import ru.pavlig43.itemlist.internal.ui.settings.SettingsRow


private const val TYPE_TRANSACTION = "Тип транзакции"
private const val TYPE_OPERATION = "Тип операции"
private val columnDefinition = listOf<ColumnDefinition<TransactionItemUi>>(
    ColumnDefinition(
        title = ID,
        width = ID_WIDTH,
        valueProvider = { it.id.toString() }
    ),
    ColumnDefinition(
        title = "V",
        width = ID_WIDTH,
        valueProvider = { if (it.isCompleted) "V" else "Ø" },
        colorProvider = { if (it.isCompleted) Color.Green else Color.Red }
    ),
    ColumnDefinition(
        title = CREATED_AT,
        width = CREATED_AT_WIDTH,
        valueProvider = { it.createdAt.convertToDateOrDateTimeString(DateFieldKind.Date) }
    ),

    ColumnDefinition(
        title = TYPE_TRANSACTION,
        width = TYPE_WIDTH,
        valueProvider = { it.transactionType.displayName }
    ),
    ColumnDefinition(
        title = TYPE_OPERATION,
        width = TYPE_WIDTH,
        valueProvider = { it.operationType.displayName }
    ),

    ColumnDefinition(
        title = COMMENT,
        width = COMMENT_WIDTH,
        valueProvider = { it.comment }
    )
)

@Composable
internal fun TransactionListScreen(
    component: TransactionListComponent,
    modifier: Modifier = Modifier
){
    Column(modifier.fillMaxWidth()) {
        SettingsRow(
            onCreate = { component.onCreate() }) {
            TransactionFilter(
                typeComponent = component.typeComponent,
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
private fun TransactionFilter(
    typeComponent: BaseFilterComponent<ItemFilter.Type<TransactionType>>,
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