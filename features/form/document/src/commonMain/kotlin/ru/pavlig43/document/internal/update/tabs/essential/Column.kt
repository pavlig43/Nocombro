package ru.pavlig43.document.internal.create.component

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.document.internal.DocumentField
import ru.pavlig43.document.internal.data.DocumentEssentialsUi
import ru.pavlig43.mutable.api.column.writeDateColumn
import ru.pavlig43.mutable.api.column.writeItemTypeColumn
import ru.pavlig43.mutable.api.column.writeTextColumn
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns

/**
 * Создаёт колонки для таблицы создания/редактирования документа
 *
 * @param onOpenDateDialog Callback для открытия диалога выбора даты. Принимает composeId строки
 * @param onChangeItem Callback для обновления данных документа
 */
@Suppress("LongMethod")
internal fun createDocumentColumns0(
    onOpenDateDialog: () -> Unit,
    onChangeItem: (DocumentEssentialsUi) -> Unit
): ImmutableList<ColumnSpec<DocumentEssentialsUi, DocumentField, Unit>> {
    val columns =
        editableTableColumns<DocumentEssentialsUi, DocumentField, Unit> {

            // Название документа
            writeTextColumn(
                headerText = "Название",
                column = DocumentField.DISPLAY_NAME,
                valueOf = { it.displayName },
                onChangeItem = { item, newValue ->
                    onChangeItem(item.copy(displayName = newValue))
                },
            )
            writeItemTypeColumn(
                headerText = "Тип",
                column = DocumentField.TYPE,
                valueOf = { it.type },
                options = DocumentType.entries,
                onTypeSelected = { item, type -> onChangeItem(item.copy(type = type)) }
            )

            // Дата создания
            writeDateColumn(
                headerText = "Дата создания",
                column = DocumentField.CREATED_AT,
                valueOf = { it.createdAt },
                onOpenDateDialog = { onOpenDateDialog() }
            )

            // Комментарий
            writeTextColumn(
                headerText = "Комментарий",
                column = DocumentField.COMMENT,
                valueOf = { it.comment },
                onChangeItem = { item, newValue ->
                    onChangeItem(item.copy(comment = newValue))
                }
            )
        }

    return columns
}