package ru.pavlig43.database.data.sync.mirror

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.TransactionType
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Typed JDBC-кодек одной [MirrorSyncTable].
 *
 * Кодек является единственным местом, где Kotlin row model сопоставляется с
 * колонками YDB: он определяет DDL, стабильный порядок bind-параметров и обратное
 * чтение [ResultSet]. [columnNames] должны точно соответствовать порядку параметров
 * в [bind], иначе batch UPSERT запишет значения в неверные поля.
 */
internal interface YdbMirrorRowCodec {
    val table: MirrorSyncTable
    val columnNames: List<String>

    fun bind(statement: PreparedStatement, row: MirrorSyncRow)
    fun read(resultSet: ResultSet): MirrorSyncRow

    /** Генерирует полный SELECT в том же порядке колонок, который ожидает codec. */
    fun selectAllSql(tablePath: String): String {
        return "SELECT ${columnNames.joinToString()} FROM `$tablePath`"
    }

    /** Генерирует SELECT без чтения строк для проверки таблицы и схемы. */
    fun selectProbeSql(tablePath: String): String {
        return "${selectAllSql(tablePath)} LIMIT 0"
    }

    /**
     * Генерирует typed UPSERT с явными YDB CAST для каждого JDBC-параметра.
     *
     * Явный CAST нужен JDBC-драйверу YDB, поскольку nullable и строковые значения
     * не всегда позволяют надежно вывести тип параметра.
     */
    fun upsertSql(tablePath: String): String {
        val values = columnNames.joinToString { "CAST(? AS ${columnType(it)})" }
        return """
            UPSERT INTO `$tablePath` (${columnNames.joinToString()})
            VALUES ($values)
        """.trimIndent()
    }

    /**
     * Строит serializable DML для условной записи и чтения строки-победителя.
     *
     * `UPSERT` выполняется лишь при отсутствии равной или более новой логической
     * версии. Следующий `SELECT` возвращает фактическую строку независимо от исхода,
     * что закрывает окно гонки между записью и проверкой результата.
     *
     * @param tablePath полный путь typed mirror-таблицы.
     */
    fun conditionalUpsertSql(tablePath: String): String {
        val values = columnNames.joinToString { "CAST(? AS ${columnType(it)})" }
        return """
            UPSERT INTO `$tablePath` (${columnNames.joinToString()})
            SELECT $values
            WHERE NOT EXISTS (
                SELECT sync_id FROM `$tablePath`
                WHERE sync_id = CAST(? AS Utf8)
                  AND IF(
                      deleted_at IS NOT NULL AND deleted_at > updated_at,
                      deleted_at,
                      updated_at
                  ) >= CAST(? AS Utf8)
            );
            SELECT ${columnNames.joinToString()} FROM `$tablePath`
            WHERE sync_id = CAST(? AS Utf8);
        """.trimIndent()
    }
}

internal object VendorYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.VENDOR
    override val columnNames = listOf(
        "sync_id",
        "display_name",
        "comment",
        "updated_at",
        "deleted_at",
    )

    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is VendorMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.displayName)
        statement.setString(3, row.comment)
        statement.setString(4, row.updatedAt.toString())
        statement.setString(5, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet): MirrorSyncRow {
        return VendorMirrorRow(
            syncId = resultSet.getString("sync_id"),
            displayName = resultSet.getString("display_name"),
            comment = resultSet.getString("comment"),
            updatedAt = LocalDateTime.parse(resultSet.getString("updated_at")),
            deletedAt = resultSet.getString("deleted_at")?.let(LocalDateTime::parse),
        )
    }
}

internal object BatchCostPriceYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.BATCH_COST_PRICE
    override val columnNames = listOf(
        "sync_id",
        "batch_sync_id",
        "cost_price_per_unit",
        "updated_at",
        "deleted_at",
    )

    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is BatchCostPriceMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.batchSyncId)
        statement.setLong(3, row.costPricePerUnit)
        statement.setString(4, row.updatedAt.toString())
        statement.setString(5, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet): MirrorSyncRow {
        return BatchCostPriceMirrorRow(
            syncId = resultSet.getString("sync_id"),
            batchSyncId = resultSet.getString("batch_sync_id"),
            costPricePerUnit = resultSet.getLong("cost_price_per_unit"),
            updatedAt = LocalDateTime.parse(resultSet.getString("updated_at")),
            deletedAt = resultSet.getString("deleted_at")?.let(LocalDateTime::parse),
        )
    }
}

internal object DocumentYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.DOCUMENT
    override val columnNames = listOf(
        "sync_id",
        "display_name",
        "type",
        "created_at",
        "comment",
        "updated_at",
        "deleted_at",
    )

    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is DocumentMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.displayName)
        statement.setString(3, row.type.name)
        statement.setString(4, row.createdAt.toString())
        statement.setString(5, row.comment)
        statement.setString(6, row.updatedAt.toString())
        statement.setString(7, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet): MirrorSyncRow {
        return DocumentMirrorRow(
            syncId = resultSet.getString("sync_id"),
            displayName = resultSet.getString("display_name"),
            type = enumValueOf<DocumentType>(resultSet.getString("type")),
            createdAt = LocalDate.parse(resultSet.getString("created_at")),
            comment = resultSet.getString("comment"),
            updatedAt = LocalDateTime.parse(resultSet.getString("updated_at")),
            deletedAt = resultSet.getString("deleted_at")?.let(LocalDateTime::parse),
        )
    }
}

internal object ProductYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.PRODUCT
    override val columnNames = listOf(
        "sync_id",
        "type",
        "display_name",
        "second_name",
        "created_at",
        "comment",
        "price_for_sale",
        "shelf_life_days",
        "rec_nds",
        "updated_at",
        "deleted_at",
    )

    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is ProductMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.type.name)
        statement.setString(3, row.displayName)
        statement.setString(4, row.secondName)
        statement.setString(5, row.createdAt.toString())
        statement.setString(6, row.comment)
        statement.setLong(7, row.priceForSale)
        statement.setInt(8, row.shelfLifeDays)
        statement.setInt(9, row.recNds)
        statement.setString(10, row.updatedAt.toString())
        statement.setString(11, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet): MirrorSyncRow {
        return ProductMirrorRow(
            syncId = resultSet.getString("sync_id"),
            type = enumValueOf<ProductType>(resultSet.getString("type")),
            displayName = resultSet.getString("display_name"),
            secondName = resultSet.getString("second_name"),
            createdAt = LocalDate.parse(resultSet.getString("created_at")),
            comment = resultSet.getString("comment"),
            priceForSale = resultSet.getLong("price_for_sale"),
            shelfLifeDays = resultSet.getInt("shelf_life_days"),
            recNds = resultSet.getInt("rec_nds"),
            updatedAt = LocalDateTime.parse(resultSet.getString("updated_at")),
            deletedAt = resultSet.getString("deleted_at")?.let(LocalDateTime::parse),
        )
    }
}

internal object TransactionYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.TRANSACTION
    override val columnNames = listOf(
        "sync_id",
        "transaction_type",
        "created_at",
        "comment",
        "is_completed",
        "updated_at",
        "deleted_at",
    )

    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is TransactionMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.transactionType.name)
        statement.setString(3, row.createdAt.toString())
        statement.setString(4, row.comment)
        statement.setBoolean(5, row.isCompleted)
        statement.setString(6, row.updatedAt.toString())
        statement.setString(7, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet): MirrorSyncRow {
        return TransactionMirrorRow(
            syncId = resultSet.getString("sync_id"),
            transactionType = enumValueOf<TransactionType>(resultSet.getString("transaction_type")),
            createdAt = LocalDateTime.parse(resultSet.getString("created_at")),
            comment = resultSet.getString("comment"),
            isCompleted = resultSet.getBoolean("is_completed"),
            updatedAt = LocalDateTime.parse(resultSet.getString("updated_at")),
            deletedAt = resultSet.getString("deleted_at")?.let(LocalDateTime::parse),
        )
    }
}

internal object ExperimentYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.EXPERIMENT
    override val columnNames = listOf(
        "sync_id",
        "title",
        "idea_description",
        "is_archived",
        "updated_at",
        "deleted_at",
    )

    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is ExperimentMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.title)
        statement.setString(3, row.ideaDescription)
        statement.setBoolean(4, row.isArchived)
        statement.setString(5, row.updatedAt.toString())
        statement.setString(6, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet): MirrorSyncRow {
        return ExperimentMirrorRow(
            syncId = resultSet.getString("sync_id"),
            title = resultSet.getString("title"),
            ideaDescription = resultSet.getString("idea_description"),
            isArchived = resultSet.getBoolean("is_archived"),
            updatedAt = LocalDateTime.parse(resultSet.getString("updated_at")),
            deletedAt = resultSet.getString("deleted_at")?.let(LocalDateTime::parse),
        )
    }
}

internal val supportedYdbMirrorCodecs: Map<MirrorSyncTable, YdbMirrorRowCodec> = listOf(
    VendorYdbMirrorCodec,
    DocumentYdbMirrorCodec,
    DeclarationYdbMirrorCodec,
    ProductYdbMirrorCodec,
    TransactionYdbMirrorCodec,
    ExperimentYdbMirrorCodec,
    ProductSpecificationYdbMirrorCodec,
    SafetyStockYdbMirrorCodec,
    ExperimentEntryYdbMirrorCodec,
    ExperimentReminderYdbMirrorCodec,
    ProductDeclarationYdbMirrorCodec,
    CompositionYdbMirrorCodec,
    BatchYdbMirrorCodec,
    BatchCostPriceYdbMirrorCodec,
    BatchMovementYdbMirrorCodec,
    ReminderYdbMirrorCodec,
    ExpenseYdbMirrorCodec,
    BuyYdbMirrorCodec,
    SaleYdbMirrorCodec,
    FileYdbMirrorCodec,
).associateBy(YdbMirrorRowCodec::table)

private fun columnType(columnName: String): String {
    return when (columnName) {
        "cost_price_per_unit",
        "price_for_sale",
        "reorder_point",
        "order_quantity",
        "count",
        "amount",
        "price",
        -> "Int64"
        "shelf_life_days",
        "rec_nds",
        "nds_percent",
        -> "Int32"
        "is_completed",
        "is_archived",
        "observe_from_notification",
        "is_product_in_declaration",
        -> "Bool"
        else -> "Utf8"
    }
}
