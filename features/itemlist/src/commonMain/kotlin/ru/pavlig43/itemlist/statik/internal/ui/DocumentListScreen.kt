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
import ru.pavlig43.itemlist.internal.ui.core.LabelSelectionLogic
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ru.pavlig43.itemlist.statik.internal.component.DocumentsStaticListContainer
import ru.pavlig43.itemlist.statik.internal.ui.core.StaticItemListBox


private fun TableRowScope.cells(
    item: DocumentItemUi,
    searchText: String
) {
    cell { Text(item.id.toString()) }
    cell {
        HighlightedText(
            text = item.displayName,
            searchText = searchText
        )


    }
    cell { Text(item.type.displayName) }
    cell { Text(item.createdAt.convertToDateOrDateTimeString(DateFieldKind.Date)) }
    cell {
        HighlightedText(
            text = item.comment,
            searchText = searchText
        )
    }

}

@Composable
internal fun DocumentListScreen(
    component: DocumentsStaticListContainer,
    modifier: Modifier = Modifier
){
    val headers = listOf(

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
                LabelSelectionLogic(
                    label = "Тип",
                    component = component.typeFilterComponent

                )

            },
        ),
        DataColumn(
            header = { Text("Создан") },
        ),
        DataColumn(
            header = { Text("Комментарий") },
        )
    )
    val searchText by component.searchTextFilterComponent.valueFlow.collectAsState()
    Column(modifier.fillMaxWidth()) {
        StaticItemListBox(
            listComponent = component.staticListComponent,
            headers = headers,
            contentRow = { cells(it, searchText) })
    }
}
