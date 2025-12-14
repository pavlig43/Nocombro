package ru.pavlig43.itemlist.statik.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.seanproctor.datatable.DataColumn
import com.seanproctor.datatable.TableRowScope
import ru.pavlig43.core.DateFieldKind
import ru.pavlig43.core.convertToDateOrDateTimeString
import ru.pavlig43.coreui.HighlightedText
import ru.pavlig43.itemlist.statik.internal.component.TransactionItemUi
import ru.pavlig43.itemlist.statik.internal.component.TransactionStaticListContainer
import ru.pavlig43.itemlist.statik.internal.ui.core.StaticItemListBox

private val headers = listOf(

    DataColumn(
        header = { Text("ID") },
    ),
    DataColumn(

        header = {
            Text("Проведен")
        },
    ),
    DataColumn(
        header = { Text("Создан") },
    ),
    DataColumn(
        header = {
            Text("Тип транзакции")
        },
    ),
    DataColumn(
        header = { Text("Комментарий") },
    )
)

private fun TableRowScope.cells(
    item: TransactionItemUi,
    searchText: String
) {
    cell { Text(item.id.toString()) }
    cell {
        Icon(
            imageVector = if (item.isCompleted) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (item.isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.error,
        )
    }
    cell { Text(item.createdAt.convertToDateOrDateTimeString(DateFieldKind.DateTime)) }
    cell { Text(item.transactionType.displayName) }
    cell { HighlightedText(
        text = item.comment,
        searchText = searchText
    ) }

}

@Composable
internal fun TransactionListScreen(
    component: TransactionStaticListContainer,
    modifier: Modifier = Modifier
) {
    val searchText by component.searchTextFilterComponent.valueFlow.collectAsState()
    Column(modifier.fillMaxWidth()) {

        StaticItemListBox(
            listComponent = component.staticListComponent,
            headers = headers,
            contentRow = { cells(it,searchText) }
        )

    }
}

