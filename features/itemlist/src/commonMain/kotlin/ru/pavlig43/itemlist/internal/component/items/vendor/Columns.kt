package ru.pavlig43.itemlist.internal.component.items.vendor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.itemlist.internal.component.SelectionUiEvent
import ru.pavlig43.itemlist.internal.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

internal enum class VendorField {

    SELECTION,
    NAME,

    ID,
    COMMENT
}

internal fun createVendorColumn(
    onEvent: (SelectionUiEvent) -> Unit,
): ImmutableList<ColumnSpec<VendorTableUi, VendorField, TableData<VendorTableUi>>> {
    val columns =
        tableColumns<VendorTableUi, VendorField, TableData<VendorTableUi>> {


            column(VendorField.SELECTION, valueOf = { it.composeId }) {

                title { "" }
                autoWidth(48.dp)
                cell { doc, tableData ->
                    if (tableData.isSelectionMode){
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Checkbox(
                                checked = doc.composeId in tableData.selectedIds,
                                onCheckedChange = {
                                    onEvent(SelectionUiEvent.ToggleSelection(doc.composeId))
                                },
                            )
                        }
                    }


                }

            }

            column(VendorField.ID, valueOf = { it.composeId }) {
                header("Ид")
                align(Alignment.Center)
                cell { document, _ -> Text(document.composeId.toString()) }
                // Enable built‑in Text filter UI in header
                // Auto‑fit to content with optional max cap
                autoWidth(max = 500.dp)

            }
            column(VendorField.NAME, valueOf = { it.displayName }) {
                header("Название")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item.displayName) }
                sortable()
            }
            column(VendorField.COMMENT, valueOf = { it.comment }) {
                header("Комментарий")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { document, _ -> Text(document.comment) }
            }
        }
    return columns

}