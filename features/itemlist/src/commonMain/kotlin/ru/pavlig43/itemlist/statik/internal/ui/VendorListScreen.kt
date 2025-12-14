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
import ru.pavlig43.coreui.HighlightedText
import ru.pavlig43.itemlist.statik.internal.component.VendorItemUi
import ru.pavlig43.itemlist.statik.internal.component.VendorStaticListContainer
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
        header = { Text("Комментарий") },
    )
)

private fun TableRowScope.cells(
    item: VendorItemUi,
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
            text = item.comment,
            searchText = searchText
        )
    }

}

@Composable
internal fun VendorListScreen(
    component: VendorStaticListContainer,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {

        val searchText by component.searchTextFilterComponent.valueFlow.collectAsState()
        StaticItemListBox(
            listComponent = component.staticListComponent,
            headers = headers,
            contentRow = { cells(it, searchText) })


    }
}

