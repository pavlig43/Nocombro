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
import ru.pavlig43.itemlist.statik.internal.component.ProductItemUi
import ru.pavlig43.itemlist.statik.internal.component.ProductStaticListContainer
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
            Text("Тип продукта")
        },
    ),
    DataColumn(
        header = { Text("Создан") },
    ),
    DataColumn(
        header = { Text("Комментарий") },
    )
)

private fun TableRowScope.productCells(
    product: ProductItemUi,
    searchText: String
) {
    cell { Text(product.id.toString()) }
    cell {
        HighlightedText(
            text = product.displayName,
            searchText = searchText
        )


    }
    cell { Text(product.type.displayName) }
    cell { Text(product.createdAt.convertToDateOrDateTimeString(DateFieldKind.Date)) }
    cell {
        HighlightedText(
            text = product.comment,
            searchText = searchText
        )
    }

}

@Composable
internal fun ProductListScreen(
    component: ProductStaticListContainer,
    modifier: Modifier = Modifier
) {
    val searchText by component.searchTextFilterComponent.valueFlow.collectAsState()
    Column(modifier.fillMaxWidth()) {

        StaticItemListBox(
            listComponent = component.staticListComponent,
            headers = headers,
            contentRow = { productCells(it, searchText) }
        )
    }
}

