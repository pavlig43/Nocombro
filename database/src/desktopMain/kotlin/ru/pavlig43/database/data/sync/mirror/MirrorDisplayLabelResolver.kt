package ru.pavlig43.database.data.sync.mirror

import ru.pavlig43.database.data.files.OwnerType

/**
 * Строит человекочитаемые подписи mirror rows для диагностики и аудита.
 *
 * Resolver объединяет remote и local snapshots по `table + sync_id`. Локальная
 * строка добавляется последней и имеет приоритет, поскольку обычно содержит
 * наиболее знакомое пользователю актуальное название. Ссылочные подписи разрешаются
 * через `*_sync_id`; при отсутствии зависимости используется сам sync id.
 */
class MirrorDisplayLabelResolver(
    localSnapshot: MirrorLocalSnapshot,
    remoteSnapshot: MirrorRemoteSnapshot,
) {
    private val rowsByKey = buildMap {
        MirrorSyncTable.mirroredBusinessTables.forEach { table ->
            remoteSnapshot.rowsByTable[table].orEmpty().forEach { put(RowKey(table, it.syncId), it) }
            localSnapshot.rowsByTable[table].orEmpty().forEach { put(RowKey(table, it.syncId), it) }
        }
    }

    /**
     * Возвращает label конкретной typed row.
     *
     * Если [row] отсутствует, возвращается стабильный fallback
     * `tableName:syncId`, пригодный для логов.
     */
    fun resolve(
        table: MirrorSyncTable,
        row: MirrorSyncRow?,
        syncId: String,
    ): String {
        if (row == null) return "${table.tableName}:$syncId"
        return when (row) {
            is VendorMirrorRow -> row.displayName
            is DocumentMirrorRow -> row.displayName
            is DeclarationMirrorRow -> "${row.displayName} · ${row.vendorName}"
            is ProductMirrorRow -> row.displayName
            is ProductSpecificationMirrorRow ->
                "Спецификация · ${nameOf(MirrorSyncTable.PRODUCT, row.productSyncId)}"
            is SafetyStockMirrorRow ->
                "Страховой запас · ${nameOf(MirrorSyncTable.PRODUCT, row.productSyncId)}"
            is CompositionMirrorRow ->
                "${nameOf(MirrorSyncTable.PRODUCT, row.parentSyncId)} ← " +
                    nameOf(MirrorSyncTable.PRODUCT, row.productSyncId)
            is ProductDeclarationMirrorRow ->
                "${nameOf(MirrorSyncTable.PRODUCT, row.productSyncId)} · " +
                    nameOf(MirrorSyncTable.DECLARATION, row.declarationSyncId)
            is BatchMirrorRow ->
                "Партия · ${nameOf(MirrorSyncTable.PRODUCT, row.productSyncId)} · ${row.dateBorn}"
            is BatchCostPriceMirrorRow ->
                "Себестоимость · ${nameOf(MirrorSyncTable.BATCH, row.batchSyncId)}"
            is BatchMovementMirrorRow ->
                "${row.movementType} · ${nameOf(MirrorSyncTable.BATCH, row.batchSyncId)} · " +
                    nameOf(MirrorSyncTable.TRANSACTION, row.transactionSyncId)
            is BuyMirrorRow ->
                "Покупка · ${nameOf(MirrorSyncTable.TRANSACTION, row.transactionSyncId)}"
            is SaleMirrorRow ->
                "Продажа · ${nameOf(MirrorSyncTable.TRANSACTION, row.transactionSyncId)} · " +
                    nameOf(MirrorSyncTable.VENDOR, row.clientSyncId)
            is ReminderMirrorRow ->
                "${row.text} · ${nameOf(MirrorSyncTable.TRANSACTION, row.transactionSyncId)}"
            is ExpenseMirrorRow -> "${row.expenseType} · ${row.comment.ifBlank { row.amount.toString() }}"
            is ExperimentMirrorRow -> row.title
            is ExperimentEntryMirrorRow ->
                "${nameOf(MirrorSyncTable.EXPERIMENT, row.experimentSyncId)} · ${row.entryDate} · " +
                    row.content.take(LABEL_TEXT_LIMIT)
            is ExperimentReminderMirrorRow ->
                "${row.text} · ${nameOf(MirrorSyncTable.EXPERIMENT, row.experimentSyncId)}"
            is TransactionMirrorRow ->
                "${row.transactionType} · ${row.createdAt.date} · ${row.comment.ifBlank { row.syncId }}"
            is FileMirrorRow ->
                "${row.displayName} · ${row.ownerType} · " +
                    nameOf(row.ownerType.mirrorTable(), row.ownerSyncId)
        }
    }

    private fun nameOf(table: MirrorSyncTable?, syncId: String): String {
        val row = table?.let { rowsByKey[RowKey(it, syncId)] }
        return when (row) {
            is VendorMirrorRow -> row.displayName
            is DocumentMirrorRow -> row.displayName
            is DeclarationMirrorRow -> row.displayName
            is ProductMirrorRow -> row.displayName
            is BatchMirrorRow ->
                "Партия ${nameOf(MirrorSyncTable.PRODUCT, row.productSyncId)} от ${row.dateBorn}"
            is TransactionMirrorRow -> "${row.transactionType} от ${row.createdAt.date}"
            is ExperimentMirrorRow -> row.title
            is ExperimentEntryMirrorRow ->
                "${nameOf(MirrorSyncTable.EXPERIMENT, row.experimentSyncId)} от ${row.entryDate}"
            is ExpenseMirrorRow -> row.comment.ifBlank { row.expenseType.toString() }
            else -> syncId
        }
    }
}

private data class RowKey(
    val table: MirrorSyncTable,
    val syncId: String,
)

private fun OwnerType.mirrorTable(): MirrorSyncTable? = when (this) {
    OwnerType.DECLARATION -> MirrorSyncTable.DECLARATION
    OwnerType.PRODUCT -> MirrorSyncTable.PRODUCT
    OwnerType.VENDOR -> MirrorSyncTable.VENDOR
    OwnerType.DOCUMENT -> MirrorSyncTable.DOCUMENT
    OwnerType.TRANSACTION -> MirrorSyncTable.TRANSACTION
    OwnerType.EXPENSE -> MirrorSyncTable.EXPENSE
    OwnerType.EXPERIMENT_ENTRY -> MirrorSyncTable.EXPERIMENT_ENTRY
}

private const val LABEL_TEXT_LIMIT = 40
