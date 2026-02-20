package ru.pavlig43.product.internal.create.component

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.mutable.api.column.writeDateColumn
import ru.pavlig43.mutable.api.column.writeItemTypeColumn
import ru.pavlig43.mutable.api.column.writeTextColumn
import ru.pavlig43.product.internal.ProductField
import ru.pavlig43.product.internal.model.ProductEssentialsUi
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns

/**
 * Создаёт колонки для таблицы создания продукта
 *
 * @param onOpenDateDialog Callback для открытия диалога выбора даты
 * @param onChangeItem Callback для обновления данных продукта
 */
@Suppress("LongMethod")
internal fun createProductColumns0(
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
                isSortable = false,
                onChangeItem = { item, newValue ->
                    onChangeItem(item.copy(displayName = newValue))
                },
            )

            // Тип продукта
            writeItemTypeColumn(
                headerText = "Тип продукта",
                column = ProductField.PRODUCT_TYPE,
                valueOf = { it.productType },
                options = ProductType.entries,
                isSortable = false,
                onTypeSelected = { item, type -> onChangeItem(item.copy(productType = type)) }
            )

            // Дата создания
            writeDateColumn(
                headerText = "Дата создания",
                column = ProductField.CREATED_AT,
                valueOf = { it.createdAt },
                isSortable = false,
                onOpenDateDialog = { onOpenDateDialog() }
            )

            // Комментарий
            writeTextColumn(
                headerText = "Комментарий",
                column = ProductField.COMMENT,
                valueOf = { it.comment },
                isSortable = false,
                onChangeItem = { item, newValue ->
                    onChangeItem(item.copy(comment = newValue))
                }
            )
        }

    return columns
}
