package ru.pavlig43.product.internal.update.tabs.essential

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.mutable.api.column.readDateColumn
import ru.pavlig43.mutable.api.column.readItemTypeColumn
import ru.pavlig43.mutable.api.column.writeDateColumn
import ru.pavlig43.mutable.api.column.writeItemTypeColumn
import ru.pavlig43.mutable.api.column.writeTextColumn
import ru.pavlig43.product.internal.ProductField
import ru.pavlig43.product.internal.model.ProductEssentialsUi
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns

/**
 * Создаёт колонки для таблицы редактирования продукта
 *
 * В режиме редактирования дата создания (createdAt) доступна только для чтения.
 *
 * @param onOpenDateDialog Callback для открытия диалога выбора даты
 * @param onChangeItem Callback для обновления данных продукта
 */
@Suppress("LongMethod")
internal fun createProductColumns1(
    onOpenDateDialog: () -> Unit,
    onChangeItem: (ProductEssentialsUi) -> Unit
): ImmutableList<ColumnSpec<ProductEssentialsUi, ProductField, Unit>> {
    val columns =
        editableTableColumns<ProductEssentialsUi, ProductField, Unit> {

            // Название продукта
            writeTextColumn(
                headerText = "Название продукта",
                column = ProductField.DISPLAY_NAME,
                valueOf = { it.displayName },
                onChangeItem = { item, newValue ->
                    onChangeItem(item.copy(displayName = newValue))
                },
            )

            // Тип продукта
            readItemTypeColumn(
                headerText = "Тип продукта",
                column = ProductField.PRODUCT_TYPE,
                valueOf = { it.productType },
            )

            writeDateColumn(
                headerText = "Дата создания",
                column = ProductField.CREATED_AT,
                valueOf = { it.createdAt },
                onOpenDateDialog = { onOpenDateDialog() }
            )

            // Комментарий
            writeTextColumn(
                headerText = "Комментарий",
                column = ProductField.COMMENT,
                valueOf = { it.comment },
                onChangeItem = { item, newValue ->
                    onChangeItem(item.copy(comment = newValue))
                }
            )
        }

    return columns
}
