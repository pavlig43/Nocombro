@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.database.data.sync.mirror

import ru.pavlig43.database.data.files.OwnerType

internal data class MirrorEntityKey(
    val table: MirrorSyncTable,
    val syncId: String,
)

internal fun MirrorPushEntityChange.entityKey() = MirrorEntityKey(table, row.syncId)

@Suppress("CyclomaticComplexMethod")
internal fun MirrorSyncRow.dependencyKeys(): List<MirrorEntityKey> = when (this) {
    is DeclarationMirrorRow -> listOf(MirrorEntityKey(MirrorSyncTable.VENDOR, vendorSyncId))
    is ProductSpecificationMirrorRow -> listOf(MirrorEntityKey(MirrorSyncTable.PRODUCT, productSyncId))
    is SafetyStockMirrorRow -> listOf(MirrorEntityKey(MirrorSyncTable.PRODUCT, productSyncId))
    is ExperimentEntryMirrorRow -> listOf(MirrorEntityKey(MirrorSyncTable.EXPERIMENT, experimentSyncId))
    is ExperimentReminderMirrorRow -> listOf(MirrorEntityKey(MirrorSyncTable.EXPERIMENT, experimentSyncId))
    is ProductDeclarationMirrorRow -> listOf(
        MirrorEntityKey(MirrorSyncTable.PRODUCT, productSyncId),
        MirrorEntityKey(MirrorSyncTable.DECLARATION, declarationSyncId),
    )
    is CompositionMirrorRow -> listOf(
        MirrorEntityKey(MirrorSyncTable.PRODUCT, parentSyncId),
        MirrorEntityKey(MirrorSyncTable.PRODUCT, productSyncId),
    )
    is BatchMirrorRow -> listOf(
        MirrorEntityKey(MirrorSyncTable.PRODUCT, productSyncId),
        MirrorEntityKey(MirrorSyncTable.DECLARATION, declarationSyncId),
    )
    is BatchCostPriceMirrorRow -> listOf(MirrorEntityKey(MirrorSyncTable.BATCH, batchSyncId))
    is BatchMovementMirrorRow -> listOf(
        MirrorEntityKey(MirrorSyncTable.BATCH, batchSyncId),
        MirrorEntityKey(MirrorSyncTable.TRANSACTION, transactionSyncId),
    )
    is ReminderMirrorRow -> listOf(MirrorEntityKey(MirrorSyncTable.TRANSACTION, transactionSyncId))
    is ExpenseMirrorRow -> transactionSyncId
        ?.let { listOf(MirrorEntityKey(MirrorSyncTable.TRANSACTION, it)) }
        .orEmpty()
    is BuyMirrorRow -> listOf(
        MirrorEntityKey(MirrorSyncTable.TRANSACTION, transactionSyncId),
        MirrorEntityKey(MirrorSyncTable.BATCH_MOVEMENT, movementSyncId),
    )
    is SaleMirrorRow -> listOf(
        MirrorEntityKey(MirrorSyncTable.TRANSACTION, transactionSyncId),
        MirrorEntityKey(MirrorSyncTable.BATCH_MOVEMENT, movementSyncId),
        MirrorEntityKey(MirrorSyncTable.VENDOR, clientSyncId),
    )
    is FileMirrorRow -> listOf(MirrorEntityKey(ownerType.ownerMirrorSyncTable(), ownerSyncId))
    is VendorMirrorRow,
    is DocumentMirrorRow,
    is ProductMirrorRow,
    is TransactionMirrorRow,
    is ExperimentMirrorRow,
    -> emptyList()
}

internal fun MirrorSyncRow.dependsOn(parent: MirrorEntityKey): Boolean =
    dependencyKeys().any { it == parent }

private fun OwnerType.ownerMirrorSyncTable(): MirrorSyncTable = when (this) {
    OwnerType.DECLARATION -> MirrorSyncTable.DECLARATION
    OwnerType.PRODUCT -> MirrorSyncTable.PRODUCT
    OwnerType.VENDOR -> MirrorSyncTable.VENDOR
    OwnerType.DOCUMENT -> MirrorSyncTable.DOCUMENT
    OwnerType.TRANSACTION -> MirrorSyncTable.TRANSACTION
    OwnerType.EXPENSE -> MirrorSyncTable.EXPENSE
    OwnerType.EXPERIMENT_ENTRY -> MirrorSyncTable.EXPERIMENT_ENTRY
}
