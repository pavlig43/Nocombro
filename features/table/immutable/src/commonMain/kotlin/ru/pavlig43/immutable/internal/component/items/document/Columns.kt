@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.document

import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.immutable.internal.column.idWithSelection
import ru.pavlig43.tablecore.model.TableData
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
    listTypeForFilter: List<DocumentType>,
    onEvent: (ImmutableTableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<DocumentTableUi, DocumentField, TableData<DocumentTableUi>>> {
    val columns =
        tableColumns<DocumentTableUi, DocumentField, TableData<DocumentTableUi>> {

            idWithSelection(
                selectionKey = DocumentField.SELECTION,
                idKey = DocumentField.ID,
                onEvent = onEvent
            )

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