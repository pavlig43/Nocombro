@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.declaration

import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.immutable.internal.ui.idWithSelection
import ru.pavlig43.tablecore.model.TableData
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
    onEvent: (ImmutableTableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<DeclarationTableUi, DeclarationField, TableData<DeclarationTableUi>>> {
    val columns =
        tableColumns<DeclarationTableUi, DeclarationField, TableData<DeclarationTableUi>> {

            idWithSelection(
                selectionKey = DeclarationField.SELECTION,
                idKey = DeclarationField.ID,
                onEvent = onEvent
            )

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