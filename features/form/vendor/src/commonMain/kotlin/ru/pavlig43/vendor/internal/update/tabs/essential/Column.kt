package ru.pavlig43.vendor.internal.update.tabs.essential

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.mutable.api.column.writeTextColumn
import ru.pavlig43.vendor.internal.VendorField
import ru.pavlig43.vendor.internal.model.VendorEssentialsUi
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns

/**
 * Создаёт колонки для таблицы редактирования поставщика
 *
 * @param onChangeItem Callback для обновления данных поставщика
 */
@Suppress("LongMethod")
internal fun createVendorColumns1(
    onChangeItem: (VendorEssentialsUi) -> Unit
): ImmutableList<ColumnSpec<VendorEssentialsUi, VendorField, Unit>> {
    val columns =
        editableTableColumns<VendorEssentialsUi, VendorField, Unit> {

            // Название поставщика
            writeTextColumn(
                headerText = "Название поставщика",
                column = VendorField.DISPLAY_NAME,
                valueOf = { it.displayName },
                isSortable = false,
                onChangeItem = { item, newValue ->
                    onChangeItem(item.copy(displayName = newValue))
                },
            )

            // Комментарий
            writeTextColumn(
                headerText = "Комментарий",
                column = VendorField.COMMENT,
                valueOf = { it.comment },
                isSortable = false,
                onChangeItem = { item, newValue ->
                    onChangeItem(item.copy(comment = newValue))
                }
            )
        }

    return columns
}
