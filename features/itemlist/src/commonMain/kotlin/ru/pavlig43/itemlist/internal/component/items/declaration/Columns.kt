package ru.pavlig43.itemlist.internal.component.items.declaration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ru.pavlig43.itemlist.internal.component.SelectionUiEvent
import ru.pavlig43.itemlist.internal.model.TableData
import ru.pavlig43.itemlist.internal.ui.createButtonNew
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

internal enum class DeclarationField {

    SELECTION,

    ID,
    NAME,
    VENDOR_NAME,
    BEST_BEFORE,
    CREATED_AT
}

@Suppress("LongMethod")
internal fun createDeclarationColumn(
    onCreate: () -> Unit,
    onEvent: (SelectionUiEvent) -> Unit,
): ImmutableList<ColumnSpec<DeclarationItemUi, DeclarationField, TableData<DeclarationItemUi>>> {
    val columns =
        tableColumns<DeclarationItemUi, DeclarationField, TableData<DeclarationItemUi>> {


            column(DeclarationField.SELECTION, valueOf = { it.id }) {

                title { createButtonNew(onCreate) }
                autoWidth(48.dp)
                cell { doc, tableData ->
                    if (tableData.isSelectionMode){
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Checkbox(
                                checked = doc.id in tableData.selectedIds,
                                onCheckedChange = {
                                    onEvent(SelectionUiEvent.ToggleSelection(doc.id))
                                },
                            )
                        }
                    }


                }

            }

            column(DeclarationField.ID, valueOf = { it.id }) {
                header("Ид")
                align(Alignment.Center)
                cell { document, _ -> Text(document.id.toString()) }
                // Enable built‑in Text filter UI in header
                // Auto‑fit to content with optional max cap
                autoWidth(max = 500.dp)

            }

            column(DeclarationField.NAME, valueOf = { it.displayName }) {
                header("Название")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { declaration, _ -> Text(declaration.displayName) }
                sortable()
            }
            column(DeclarationField.VENDOR_NAME, valueOf = { it.vendorName }) {
                header("Поставщик")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { declaration, _ -> Text(declaration.vendorName) }
                sortable()
            }

            column(DeclarationField.CREATED_AT, valueOf = { it.createdAt }) {
                header("Создан")
                align(Alignment.Center)
                filter(TableFilterType.DateTableFilter())
                cell { declaration, _ ->
                    Text(
                        declaration.createdAt.format(dateFormat)
                    )
                }
                sortable()
            }
            column(DeclarationField.BEST_BEFORE, valueOf = { it.bestBefore }) {
                header("Годна до")
                align(Alignment.Center)
                filter(TableFilterType.DateTableFilter())
                cell { declaration, _ ->
                    Text(
                        declaration.createdAt.format(dateFormat)
                    )
                }
                sortable()
            }
        }
    return columns

}