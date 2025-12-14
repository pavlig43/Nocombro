package ru.pavlig43.itemlist.statik.internal.ui


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import ru.pavlig43.itemlist.statik.internal.component.DeclarationItemUi
import ru.pavlig43.itemlist.statik.internal.component.DeclarationStaticListContainer
import ru.pavlig43.itemlist.statik.internal.ui.core.StaticItemListBox

private val headers = listOf(

    DataColumn(
        header = { Text("ID") },
    ),
    DataColumn(

        header = {
            Text("Название")
        },
    ),
    DataColumn(
        header = {
            Text("Поставщик")
        },
    ),
    DataColumn(
        header = { Text("Создан") },
    ),
    DataColumn(
        header = { Text("Годен до") },
    )
)

private fun TableRowScope.cells(
    item: DeclarationItemUi,
    searchText: String
) {
    cell { Text(item.id.toString()) }
    cell {
        HighlightedText(
            text = item.displayName,
            searchText = searchText
        )

    }
    cell {
        HighlightedText(
            text = item.vendorName,
            searchText = searchText
        )
    }
    cell { Text(item.createdAt.convertToDateOrDateTimeString(DateFieldKind.Date)) }
    cell { Text(item.bestBefore.convertToDateOrDateTimeString(DateFieldKind.Date)) }


}


@Composable
internal fun DeclarationListScreen(
    component: DeclarationStaticListContainer,
    modifier: Modifier = Modifier
) {
    val searchText by component.searchTextFilterComponent.valueFlow.collectAsState()
    Column(modifier.fillMaxWidth()) {

        val searchText by component.searchTextFilterComponent.valueFlow.collectAsState()
        StaticItemListBox(
            listComponent = component.staticListComponent,
            headers = headers,
            contentRow = { cells(it, searchText) })

    }
}

