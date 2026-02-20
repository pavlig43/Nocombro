package ru.pavlig43.document.internal.update.tabs.essential

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.document.internal.DocumentField
import ru.pavlig43.document.internal.model.DocumentEssentialsUi
import ru.pavlig43.mutable.api.column.readItemTypeColumn
import ru.pavlig43.mutable.api.column.writeDateColumn
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
internal fun createDocumentColumns1(
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
                isSortable = false,
                onChangeItem = { item, newValue ->
                    onChangeItem(item.copy(displayName = newValue))
                },
            )
            readItemTypeColumn(
                headerText = "Тип",
                column = DocumentField.TYPE,
                valueOf = { it.type },
                isSortable = false,
            )


            // Дата создания
            writeDateColumn(
                headerText = "Дата создания",
                column = DocumentField.CREATED_AT,
                valueOf = { it.createdAt },
                isSortable = false,
                onOpenDateDialog = { onOpenDateDialog() }
            )

            // Комментарий
            writeTextColumn(
                headerText = "Комментарий",
                column = DocumentField.COMMENT,
                valueOf = { it.comment },
                isSortable = false,
                onChangeItem = { item, newValue ->
                    onChangeItem(item.copy(comment = newValue))
                }
            )
        }

    return columns
}