package ru.pavlig43.database.data.sync.mirror

import ru.pavlig43.database.data.batch.BATCH_MOVEMENT_TABLE_NAME
import ru.pavlig43.database.data.batch.BATCH_TABLE_NAME
import ru.pavlig43.database.data.batch.BATCH_COST_PRICE_TABLE_NAME
import ru.pavlig43.database.data.declaration.DECLARATIONS_TABLE_NAME
import ru.pavlig43.database.data.document.DOCUMENT_TABLE_NAME
import ru.pavlig43.database.data.expense.EXPENSE_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_ENTRY_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_REMINDER_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_TABLE_NAME
import ru.pavlig43.database.data.files.FILE_TABLE_NAME
import ru.pavlig43.database.data.product.COMPOSITION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_DECLARATION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_SPECIFICATION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.product.SAFETY_STOCK_TABLE_NAME
import ru.pavlig43.database.data.transact.TRANSACTION_TABLE_NAME
import ru.pavlig43.database.data.transact.buy.BUY_TABLE_NAME
import ru.pavlig43.database.data.transact.reminder.REMINDER_TABLE_NAME
import ru.pavlig43.database.data.transact.sale.SALE_TABLE_NAME
import ru.pavlig43.database.data.vendor.VENDOR_TABLE_NAME

/**
 * Канонический инвентарь business tables, участвующих в mirror sync v1.
 *
 * [tableName] совпадает с именем локальной Room-таблицы и используется как имя
 * typed table в YDB. [applyOrder] задает порядок зависимостей: активные строки
 * применяются от родителей к детям, а tombstone в обратном порядке. При добавлении
 * таблицы порядок необходимо выбирать с учетом внешних ключей и одновременно
 * добавлять row model, mapper, local apply и YDB codec.
 */
enum class MirrorSyncTable(
    val tableName: String,
    val applyOrder: Int,
) {
    VENDOR(VENDOR_TABLE_NAME, 0),
    DOCUMENT(DOCUMENT_TABLE_NAME, 1),
    DECLARATION(DECLARATIONS_TABLE_NAME, 2),
    PRODUCT(PRODUCT_TABLE_NAME, 3),
    TRANSACTION(TRANSACTION_TABLE_NAME, 4),
    EXPERIMENT(EXPERIMENT_TABLE_NAME, 5),
    PRODUCT_SPECIFICATION(PRODUCT_SPECIFICATION_TABLE_NAME, 6),
    SAFETY_STOCK(SAFETY_STOCK_TABLE_NAME, 7),
    EXPERIMENT_ENTRY(EXPERIMENT_ENTRY_TABLE_NAME, 8),
    EXPERIMENT_REMINDER(EXPERIMENT_REMINDER_TABLE_NAME, 9),
    PRODUCT_DECLARATION(PRODUCT_DECLARATION_TABLE_NAME, 10),
    COMPOSITION(COMPOSITION_TABLE_NAME, 11),
    BATCH(BATCH_TABLE_NAME, 12),
    BATCH_COST_PRICE(BATCH_COST_PRICE_TABLE_NAME, 13),
    BATCH_MOVEMENT(BATCH_MOVEMENT_TABLE_NAME, 14),
    REMINDER(REMINDER_TABLE_NAME, 15),
    EXPENSE(EXPENSE_TABLE_NAME, 16),
    BUY(BUY_TABLE_NAME, 17),
    SALE(SALE_TABLE_NAME, 18),
    FILE(FILE_TABLE_NAME, 19),
    ;

    companion object {
        /** Все синхронизируемые таблицы в безопасном порядке upsert. */
        val mirroredBusinessTables: List<MirrorSyncTable> = entries.sortedBy(MirrorSyncTable::applyOrder)

        /** Находит enum по физическому имени Room/YDB-таблицы. */
        fun fromTableName(tableName: String): MirrorSyncTable? {
            return entries.firstOrNull { it.tableName == tableName }
        }
    }
}
