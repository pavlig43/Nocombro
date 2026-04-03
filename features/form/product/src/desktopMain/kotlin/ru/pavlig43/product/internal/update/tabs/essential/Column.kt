package ru.pavlig43.product.internal.update.tabs.essential

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.intRangeColumn
import ru.pavlig43.mutable.api.column.readItemTypeColumn
import ru.pavlig43.mutable.api.column.writeDateColumn
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
    onChangeItem: ((ProductEssentialsUi) -> ProductEssentialsUi) -> Unit
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
                    onChangeItem { it.copy(displayName = newValue) }
                },
            )



            // Тип продукта
            readItemTypeColumn(
                headerText = "Тип продукта",
                column = ProductField.PRODUCT_TYPE,
                valueOf = { it.productType },
                isSortable = false,
            )

            decimalColumn(
                key = ProductField.PRICE_FOR_SALE,
                getValue = { it.priceForSale },
                headerText = "Цена продажи (₽)",
                updateItem = { item, newPrice -> onChangeItem { it.copy(priceForSale = newPrice) } },
                isSortable = false
            )

            writeDateColumn(
                headerText = "Дата создания",
                column = ProductField.CREATED_AT,
                valueOf = { it.createdAt },
                isSortable = false,
                onOpenDateDialog = { onOpenDateDialog() }
            )
            // Второе название продукта
            writeTextColumn(
                headerText = "SN",
                column = ProductField.SECOND_NAME,
                valueOf = { it.secondName },
                isSortable = false,
                onChangeItem = { item, newValue ->
                    onChangeItem { it.copy(secondName = newValue) }
                },
            )
            intRangeColumn(
                key = ProductField.SHELF_LIFE_DAYS,
                getValue = {it.shelfLifeDays},
                headerText = "Срок годности",
                range = (0..Int.MAX_VALUE),
                updateItem = { item, newValue ->
                    onChangeItem { it.copy(shelfLifeDays = newValue) }
                },
                isSortable = false
            )
            // Рекомендованный НДС
            intRangeColumn(
                key = ProductField.REC_NDS,
                getValue = { it.recNds },
                headerText = "НДС %",
                range = 0..99,
                updateItem = { item, newValue ->
                    onChangeItem { it.copy(recNds = newValue) }
                },
                isSortable = false,
                placeholder = "0"
            )

            // Комментарий
            writeTextColumn(
                headerText = "Комментарий",
                column = ProductField.COMMENT,
                valueOf = { it.comment },
                isSortable = false,
                onChangeItem = { item, newValue ->
                    onChangeItem { it.copy(comment = newValue) }
                }
            )


        }

    return columns
}
