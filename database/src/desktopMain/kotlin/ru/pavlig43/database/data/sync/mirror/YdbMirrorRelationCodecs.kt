package ru.pavlig43.database.data.sync.mirror

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.files.OwnerType
import java.sql.PreparedStatement
import java.sql.ResultSet

internal object DeclarationYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.DECLARATION
    override val columnNames = listOf(
        "sync_id", "display_name", "created_at", "vendor_sync_id", "vendor_name",
        "born_date", "best_before", "observe_from_notification", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is DeclarationMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.displayName)
        statement.setString(3, row.createdAt.toString())
        statement.setString(4, row.vendorSyncId)
        statement.setString(5, row.vendorName)
        statement.setString(6, row.bornDate.toString())
        statement.setString(7, row.bestBefore.toString())
        statement.setBoolean(8, row.observeFromNotification)
        statement.setString(9, row.updatedAt.toString())
        statement.setString(10, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = DeclarationMirrorRow(
        syncId = resultSet.getString("sync_id"),
        displayName = resultSet.getString("display_name"),
        createdAt = LocalDate.parse(resultSet.getString("created_at")),
        vendorSyncId = resultSet.getString("vendor_sync_id"),
        vendorName = resultSet.getString("vendor_name"),
        bornDate = LocalDate.parse(resultSet.getString("born_date")),
        bestBefore = LocalDate.parse(resultSet.getString("best_before")),
        observeFromNotification = resultSet.getBoolean("observe_from_notification"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

internal object ProductSpecificationYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.PRODUCT_SPECIFICATION
    override val columnNames = listOf(
        "sync_id", "product_sync_id", "dosage", "composition", "shelf_life_text",
        "storage_conditions", "appearance", "color", "smell", "taste",
        "physical_chemical_indicators", "microbiological_indicators", "toxic_elements",
        "allergens", "gmo_info", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is ProductSpecificationMirrorRow)
        val values = listOf(
            row.syncId, row.productSyncId, row.dosage, row.composition, row.shelfLifeText,
            row.storageConditions, row.appearance, row.color, row.smell, row.taste,
            row.physicalChemicalIndicators, row.microbiologicalIndicators, row.toxicElements,
            row.allergens, row.gmoInfo, row.updatedAt.toString(), row.deletedAt?.toString(),
        )
        values.forEachIndexed { index, value -> statement.setString(index + 1, value) }
    }

    override fun read(resultSet: ResultSet) = ProductSpecificationMirrorRow(
        syncId = resultSet.getString("sync_id"),
        productSyncId = resultSet.getString("product_sync_id"),
        dosage = resultSet.getString("dosage"),
        composition = resultSet.getString("composition"),
        shelfLifeText = resultSet.getString("shelf_life_text"),
        storageConditions = resultSet.getString("storage_conditions"),
        appearance = resultSet.getString("appearance"),
        color = resultSet.getString("color"),
        smell = resultSet.getString("smell"),
        taste = resultSet.getString("taste"),
        physicalChemicalIndicators = resultSet.getString("physical_chemical_indicators"),
        microbiologicalIndicators = resultSet.getString("microbiological_indicators"),
        toxicElements = resultSet.getString("toxic_elements"),
        allergens = resultSet.getString("allergens"),
        gmoInfo = resultSet.getString("gmo_info"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

internal object SafetyStockYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.SAFETY_STOCK
    override val columnNames = listOf(
        "sync_id", "product_sync_id", "reorder_point", "order_quantity", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is SafetyStockMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.productSyncId)
        statement.setLong(3, row.reorderPoint)
        statement.setLong(4, row.orderQuantity)
        statement.setString(5, row.updatedAt.toString())
        statement.setString(6, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = SafetyStockMirrorRow(
        syncId = resultSet.getString("sync_id"),
        productSyncId = resultSet.getString("product_sync_id"),
        reorderPoint = resultSet.getLong("reorder_point"),
        orderQuantity = resultSet.getLong("order_quantity"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

internal object ExperimentEntryYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.EXPERIMENT_ENTRY
    override val columnNames = listOf(
        "sync_id", "experiment_sync_id", "entry_date", "created_at", "content", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is ExperimentEntryMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.experimentSyncId)
        statement.setString(3, row.entryDate.toString())
        statement.setString(4, row.createdAt.toString())
        statement.setString(5, row.content)
        statement.setString(6, row.updatedAt.toString())
        statement.setString(7, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = ExperimentEntryMirrorRow(
        syncId = resultSet.getString("sync_id"),
        experimentSyncId = resultSet.getString("experiment_sync_id"),
        entryDate = LocalDate.parse(resultSet.getString("entry_date")),
        createdAt = resultSet.dateTime("created_at"),
        content = resultSet.getString("content"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

internal object ExperimentReminderYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.EXPERIMENT_REMINDER
    override val columnNames = listOf(
        "sync_id", "experiment_sync_id", "text", "reminder_date_time", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is ExperimentReminderMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.experimentSyncId)
        statement.setString(3, row.text)
        statement.setString(4, row.reminderDateTime.toString())
        statement.setString(5, row.updatedAt.toString())
        statement.setString(6, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = ExperimentReminderMirrorRow(
        syncId = resultSet.getString("sync_id"),
        experimentSyncId = resultSet.getString("experiment_sync_id"),
        text = resultSet.getString("text"),
        reminderDateTime = resultSet.dateTime("reminder_date_time"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

internal object ProductDeclarationYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.PRODUCT_DECLARATION
    override val columnNames = listOf(
        "sync_id", "product_sync_id", "declaration_sync_id",
        "is_product_in_declaration", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is ProductDeclarationMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.productSyncId)
        statement.setString(3, row.declarationSyncId)
        statement.setBoolean(4, row.isProductInDeclaration)
        statement.setString(5, row.updatedAt.toString())
        statement.setString(6, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = ProductDeclarationMirrorRow(
        syncId = resultSet.getString("sync_id"),
        productSyncId = resultSet.getString("product_sync_id"),
        declarationSyncId = resultSet.getString("declaration_sync_id"),
        isProductInDeclaration = resultSet.getBoolean("is_product_in_declaration"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

internal object CompositionYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.COMPOSITION
    override val columnNames = listOf(
        "sync_id", "parent_sync_id", "product_sync_id", "count", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is CompositionMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.parentSyncId)
        statement.setString(3, row.productSyncId)
        statement.setLong(4, row.count)
        statement.setString(5, row.updatedAt.toString())
        statement.setString(6, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = CompositionMirrorRow(
        syncId = resultSet.getString("sync_id"),
        parentSyncId = resultSet.getString("parent_sync_id"),
        productSyncId = resultSet.getString("product_sync_id"),
        count = resultSet.getLong("count"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

internal object BatchYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.BATCH
    override val columnNames = listOf(
        "sync_id", "product_sync_id", "date_born", "declaration_sync_id", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is BatchMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.productSyncId)
        statement.setString(3, row.dateBorn.toString())
        statement.setString(4, row.declarationSyncId)
        statement.setString(5, row.updatedAt.toString())
        statement.setString(6, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = BatchMirrorRow(
        syncId = resultSet.getString("sync_id"),
        productSyncId = resultSet.getString("product_sync_id"),
        dateBorn = LocalDate.parse(resultSet.getString("date_born")),
        declarationSyncId = resultSet.getString("declaration_sync_id"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

internal object BatchMovementYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.BATCH_MOVEMENT
    override val columnNames = listOf(
        "sync_id", "batch_sync_id", "movement_type", "count",
        "transaction_sync_id", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is BatchMovementMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.batchSyncId)
        statement.setString(3, row.movementType.name)
        statement.setLong(4, row.count)
        statement.setString(5, row.transactionSyncId)
        statement.setString(6, row.updatedAt.toString())
        statement.setString(7, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = BatchMovementMirrorRow(
        syncId = resultSet.getString("sync_id"),
        batchSyncId = resultSet.getString("batch_sync_id"),
        movementType = enumValueOf<MovementType>(resultSet.getString("movement_type")),
        count = resultSet.getLong("count"),
        transactionSyncId = resultSet.getString("transaction_sync_id"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

internal object ReminderYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.REMINDER
    override val columnNames = listOf(
        "sync_id", "transaction_sync_id", "text", "reminder_date_time", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is ReminderMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.transactionSyncId)
        statement.setString(3, row.text)
        statement.setString(4, row.reminderDateTime.toString())
        statement.setString(5, row.updatedAt.toString())
        statement.setString(6, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = ReminderMirrorRow(
        syncId = resultSet.getString("sync_id"),
        transactionSyncId = resultSet.getString("transaction_sync_id"),
        text = resultSet.getString("text"),
        reminderDateTime = resultSet.dateTime("reminder_date_time"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

internal object ExpenseYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.EXPENSE
    override val columnNames = listOf(
        "sync_id", "transaction_sync_id", "expense_type", "amount",
        "expense_date_time", "comment", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is ExpenseMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.transactionSyncId)
        statement.setString(3, row.expenseType.name)
        statement.setLong(4, row.amount)
        statement.setString(5, row.expenseDateTime.toString())
        statement.setString(6, row.comment)
        statement.setString(7, row.updatedAt.toString())
        statement.setString(8, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = ExpenseMirrorRow(
        syncId = resultSet.getString("sync_id"),
        transactionSyncId = resultSet.getString("transaction_sync_id"),
        expenseType = enumValueOf<ExpenseType>(resultSet.getString("expense_type")),
        amount = resultSet.getLong("amount"),
        expenseDateTime = resultSet.dateTime("expense_date_time"),
        comment = resultSet.getString("comment"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

internal object BuyYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.BUY
    override val columnNames = listOf(
        "sync_id", "transaction_sync_id", "movement_sync_id", "price",
        "comment", "nds_percent", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is BuyMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.transactionSyncId)
        statement.setString(3, row.movementSyncId)
        statement.setLong(4, row.price)
        statement.setString(5, row.comment)
        statement.setInt(6, row.ndsPercent)
        statement.setString(7, row.updatedAt.toString())
        statement.setString(8, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = BuyMirrorRow(
        syncId = resultSet.getString("sync_id"),
        transactionSyncId = resultSet.getString("transaction_sync_id"),
        movementSyncId = resultSet.getString("movement_sync_id"),
        price = resultSet.getLong("price"),
        comment = resultSet.getString("comment"),
        ndsPercent = resultSet.getInt("nds_percent"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

internal object SaleYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.SALE
    override val columnNames = listOf(
        "sync_id", "transaction_sync_id", "movement_sync_id", "price",
        "comment", "client_sync_id", "nds_percent", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is SaleMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.transactionSyncId)
        statement.setString(3, row.movementSyncId)
        statement.setLong(4, row.price)
        statement.setString(5, row.comment)
        statement.setString(6, row.clientSyncId)
        statement.setInt(7, row.ndsPercent)
        statement.setString(8, row.updatedAt.toString())
        statement.setString(9, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = SaleMirrorRow(
        syncId = resultSet.getString("sync_id"),
        transactionSyncId = resultSet.getString("transaction_sync_id"),
        movementSyncId = resultSet.getString("movement_sync_id"),
        price = resultSet.getLong("price"),
        comment = resultSet.getString("comment"),
        clientSyncId = resultSet.getString("client_sync_id"),
        ndsPercent = resultSet.getInt("nds_percent"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

internal object FileYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.FILE
    override val columnNames = listOf(
        "sync_id", "owner_type", "owner_sync_id", "display_name", "path",
        "remote_object_key", "remote_storage_provider", "updated_at", "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is FileMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.ownerType.name)
        statement.setString(3, row.ownerSyncId)
        statement.setString(4, row.displayName)
        statement.setString(5, row.path)
        statement.setString(6, row.remoteObjectKey)
        statement.setString(7, row.remoteStorageProvider)
        statement.setString(8, row.updatedAt.toString())
        statement.setString(9, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = FileMirrorRow(
        syncId = resultSet.getString("sync_id"),
        ownerType = enumValueOf<OwnerType>(resultSet.getString("owner_type")),
        ownerSyncId = resultSet.getString("owner_sync_id"),
        displayName = resultSet.getString("display_name"),
        path = resultSet.getString("path"),
        remoteObjectKey = resultSet.getString("remote_object_key"),
        remoteStorageProvider = resultSet.getString("remote_storage_provider"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

private fun ResultSet.dateTime(columnName: String): LocalDateTime {
    return LocalDateTime.parse(getString(columnName))
}

private fun ResultSet.nullableDateTime(columnName: String): LocalDateTime? {
    return getString(columnName)?.let(LocalDateTime::parse)
}
