package ru.pavlig43.itemlist.refactor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

enum class DocumentField {

    SELECTION,

    ID,
    NAME,
    TYPE,
    CREATED_AT,
    COMMENT
}

fun createColumn(
    withCheckBox: Boolean,
    onEvent: (SampleUiEvent) -> Unit,
): ImmutableList<ColumnSpec<DocumentItemUi, DocumentField, TableData<DocumentItemUi>>> {
    val columns =
        tableColumns<DocumentItemUi, DocumentField, TableData<DocumentItemUi>> {

            if (withCheckBox){
                column(DocumentField.SELECTION, valueOf = { it.id }) {
                    title { "" }
                    autoWidth(48.dp)
//                width(min = 24.dp, pref = 24.dp)
                    cell { doc, tableData ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Checkbox(
                                checked = doc.id in tableData.selectedIds,
                                onCheckedChange = {
                                    onEvent(SampleUiEvent.ToggleSelection(doc.id))
                                },
                            )
                        }

                    }

                }
            }



            column(DocumentField.ID, valueOf = { it.id }) {
                header("Ид")
                cell { document, _ -> Text(document.id.toString()) }
                sortable()
                // Enable built‑in Text filter UI in header
                filter(TableFilterType.TextTableFilter())
                // Auto‑fit to content with optional max cap
                autoWidth(max = 500.dp)

            }

            column(DocumentField.NAME, valueOf = { it.displayName }) {
                header("Название")
                cell { document, _ -> Text(document.displayName) }
                sortable()
            }
            column(DocumentField.TYPE, valueOf = { it.type }) {
                header("Тип")
                cell { document, _ -> Text(document.type.displayName) }
            }
            column(DocumentField.CREATED_AT, valueOf = { it.createdAt }) {
                header("Создан")
                cell { document, _ ->
                    Text(
                        document.createdAt.toString()
                    )
                }
                sortable()
            }
            column(DocumentField.COMMENT, valueOf = { it.comment }) {
                header("Комментарий")
                cell { document, _ -> Text(document.comment) }
            }
        }
    return columns

}