package ru.pavlig43.itemlist.core.ui

import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import com.seanproctor.datatable.DataColumn
import com.seanproctor.datatable.TableRowScope
import ru.pavlig43.itemlist.core.refac.api.model.IItemUi
import ru.pavlig43.itemlist.statik.internal.component.core.SelectedRowsComponent

internal fun List<DataColumn>.withOptionalCheckboxHeader(
    withCheckbox: Boolean
): List<DataColumn> =
    if (!withCheckbox) this
    else buildList {
        add(DataColumn(header = { Text("") }))
        addAll(this@withOptionalCheckboxHeader)
    }

internal fun <U : IItemUi> rowContentWithOptionalCheckbox(
    withCheckbox: Boolean,
    selectedRowsComponent: SelectedRowsComponent,
    baseContent: TableRowScope.(U) -> Unit,
): TableRowScope.(U) -> Unit = { item ->
    if (withCheckbox) {
        cell {
            Checkbox(
                checked = item.id in selectedRowsComponent.selectedItemIds,
                onCheckedChange = {
                    selectedRowsComponent.actionInSelectedItemIds(
                        item.id,
                        it
                    )
                }
            )
        }
    }
    baseContent(item)
}