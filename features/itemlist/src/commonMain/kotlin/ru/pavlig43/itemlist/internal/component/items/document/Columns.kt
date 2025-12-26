package ru.pavlig43.itemlist.internal.component.items.document

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.internal.component.SelectionUiEvent
import ru.pavlig43.itemlist.internal.model.TableData
import ru.pavlig43.itemlist.internal.ui.createButtonNew
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

internal enum class DocumentField {

    SELECTION,

    ID,
    NAME,
    TYPE,
    CREATED_AT,
    COMMENT
}

@Suppress("LongMethod")
internal fun createDocumentColumn(
    onCreate: () -> Unit,
    listTypeForFilter: List<DocumentType>,
    onEvent: (SelectionUiEvent) -> Unit,
): ImmutableList<ColumnSpec<DocumentTableUi, DocumentField, TableData<DocumentTableUi>>> {
    val columns =
        tableColumns<DocumentTableUi, DocumentField, TableData<DocumentTableUi>> {


                column(DocumentField.SELECTION, valueOf = { it.composeId }) {

                    title { createButtonNew(onCreate) }
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

            column(DocumentField.ID, valueOf = { it.composeId }) {
                header("Ид")
                align(Alignment.Center)
                cell { document, _ -> Text(document.composeId.toString()) }
                // Enable built‑in Text filter UI in header
                // Auto‑fit to content with optional max cap
                autoWidth(max = 500.dp)

            }

            column(DocumentField.NAME, valueOf = { it.displayName }) {
                header("Название")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { document, _ -> Text(document.displayName) }
                sortable()
            }
            column(DocumentField.TYPE, valueOf = { it.type }) {
                header("Тип")
                align(Alignment.Center)
                filter(
                    TableFilterType.EnumTableFilter(
                        listTypeForFilter.toImmutableList(),
                        getTitle = { it.displayName })
                )
                cell { document, _ -> Text(document.type.displayName) }
            }
            column(DocumentField.CREATED_AT, valueOf = { it.createdAt }) {
                header("Создан")
                align(Alignment.Center)
                filter(TableFilterType.DateTableFilter())
                cell { document, _ ->
                    Text(document.createdAt.format(dateFormat))
                }
                sortable()
            }
            column(DocumentField.COMMENT, valueOf = { it.comment }) {
                header("Комментарий")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { document, _ -> Text(document.comment) }
            }
        }
    return columns

}