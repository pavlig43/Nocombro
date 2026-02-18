@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.document

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.immutable.internal.column.idWithSelection
import ru.pavlig43.immutable.internal.column.readDateColumn
import ru.pavlig43.immutable.internal.column.readEnumColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
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

            readTextColumn(
                headerText = "Название",
                column = DocumentField.NAME,
                valueOf = { it.displayName }
            )

            readEnumColumn(
                headerText = "Тип",
                column = DocumentField.TYPE,
                valueOf = { it.type },
                filterType = TableFilterType.EnumTableFilter(
                    listTypeForFilter.toImmutableList(),
                    getTitle = { it.displayName }
                ),
                getTitle = { it.displayName }
            )

            readDateColumn(
                headerText = "Создан",
                column = DocumentField.CREATED_AT,
                valueOf = { it.createdAt }
            )

            readTextColumn(
                headerText = "Комментарий",
                column = DocumentField.COMMENT,
                valueOf = { it.comment }
            )
        }
    return columns

}
