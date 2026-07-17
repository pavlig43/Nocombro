package ru.pavlig43.database.data.sync.mirror

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.datetime.getCurrentLocalDateTime

/**
 * Строит единый локальный mirror snapshot из Room-таблиц и deletion journal.
 *
 * Числовые локальные id преобразуются mapper-ами в стабильные `sync_id`, включая
 * ссылки на родительские сущности. После объединения с журналом для каждого
 * `table + sync_id` оставляется строка с максимальной логической версией.
 */
class MirrorLocalSnapshotRepository(
    private val db: NocombroDatabase,
    private val batchCostPriceMapper: BatchCostPriceMirrorMapper = BatchCostPriceMirrorMapper(db),
    private val json: Json = Json { classDiscriminator = "_mirrorType" },
) {
    /**
     * Загружает snapshot указанных таблиц, пригодный для reconciliation.
     *
     * @param tables таблицы, которые должны присутствовать в результате.
     * @return database snapshot, дополненный сериализованными tombstone журнала.
     * @throws IllegalArgumentException если mapper обнаружил нарушенный sync-инвариант.
     */
    suspend fun loadSnapshot(
        tables: List<MirrorSyncTable>,
    ): MirrorLocalSnapshot {
        val databaseSnapshot = loadDatabaseSnapshot(tables)
        val journalRows = db.mirrorDeletionJournalDao.getAll()
            .mapNotNull { entry ->
                val table = MirrorSyncTable.fromTableName(entry.entityTable) ?: return@mapNotNull null
                if (table !in tables) return@mapNotNull null
                table to json.decodeFromString<MirrorSyncRow>(entry.rowJson)
            }
            .groupBy({ it.first }, { it.second })

        return databaseSnapshot.copy(
            rowsByTable = tables.associateWith { table ->
                (databaseSnapshot.rowsByTable[table].orEmpty() + journalRows[table].orEmpty())
                    .groupBy(MirrorSyncRow::syncId)
                    .map { (_, rows) -> rows.maxBy(MirrorSyncRow::versionAt) }
            }
        )
    }

    /**
     * Загружает только физически существующие Room-строки, не читая журнал.
     *
     * Метод используется при поиске hard delete: примесь старых tombstone исказила
     * бы сравнение состояния до и после операции. [MirrorLocalSnapshot.loadedAt]
     * хранит время чтения для диагностики, а не версию бизнес-строки, поэтому здесь
     * используется обычное локальное время.
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    internal suspend fun loadDatabaseSnapshot(
        tables: List<MirrorSyncTable>,
    ): MirrorLocalSnapshot {
        val requestedTables = tables.toSet()
        return MirrorLocalSnapshot(
            loadedAt = getCurrentLocalDateTime(),
            rowsByTable = buildMap {
                if (MirrorSyncTable.VENDOR in requestedTables) {
                    put(MirrorSyncTable.VENDOR, db.vendorDao.getAll().map { it.toMirrorRow() })
                }
                if (MirrorSyncTable.DOCUMENT in requestedTables) {
                    put(MirrorSyncTable.DOCUMENT, db.documentDao.getAll().map { it.toMirrorRow() })
                }
                if (MirrorSyncTable.PRODUCT in requestedTables) {
                    put(MirrorSyncTable.PRODUCT, db.productDao.getAll().map { it.toMirrorRow() })
                }
                if (MirrorSyncTable.TRANSACTION in requestedTables) {
                    put(MirrorSyncTable.TRANSACTION, db.transactionDao.getAll().map { it.toMirrorRow() })
                }
                if (MirrorSyncTable.EXPERIMENT in requestedTables) {
                    put(MirrorSyncTable.EXPERIMENT, db.experimentDao.getAll().map { it.toMirrorRow() })
                }
                if (MirrorSyncTable.DECLARATION in requestedTables) {
                    put(
                        MirrorSyncTable.DECLARATION,
                        db.declarationDao.getAll().map { it.toMirrorRow(db) },
                    )
                }
                if (MirrorSyncTable.PRODUCT_SPECIFICATION in requestedTables) {
                    put(
                        MirrorSyncTable.PRODUCT_SPECIFICATION,
                        db.productSpecificationDao.getAll().map { it.toMirrorRow(db) },
                    )
                }
                if (MirrorSyncTable.SAFETY_STOCK in requestedTables) {
                    put(
                        MirrorSyncTable.SAFETY_STOCK,
                        db.safetyStockDao.getAll().map { it.toMirrorRow(db) },
                    )
                }
                if (MirrorSyncTable.EXPERIMENT_ENTRY in requestedTables) {
                    put(
                        MirrorSyncTable.EXPERIMENT_ENTRY,
                        db.experimentEntryDao.getAll().map { it.toMirrorRow(db) },
                    )
                }
                if (MirrorSyncTable.EXPERIMENT_REMINDER in requestedTables) {
                    put(
                        MirrorSyncTable.EXPERIMENT_REMINDER,
                        db.experimentReminderDao.getAll().map { it.toMirrorRow(db) },
                    )
                }
                if (MirrorSyncTable.PRODUCT_DECLARATION in requestedTables) {
                    put(
                        MirrorSyncTable.PRODUCT_DECLARATION,
                        db.productDeclarationDao.getAll().map { it.toMirrorRow(db) },
                    )
                }
                if (MirrorSyncTable.COMPOSITION in requestedTables) {
                    put(
                        MirrorSyncTable.COMPOSITION,
                        db.compositionDao.getAll().map { it.toMirrorRow(db) },
                    )
                }
                if (MirrorSyncTable.BATCH in requestedTables) {
                    put(
                        MirrorSyncTable.BATCH,
                        db.batchDao.getAll().map { it.toMirrorRow(db) },
                    )
                }
                if (MirrorSyncTable.BATCH_COST_PRICE in requestedTables) {
                    put(
                        MirrorSyncTable.BATCH_COST_PRICE,
                        db.batchCostDao.getAll().map { batchCostPriceMapper.toMirrorRow(it) },
                    )
                }
                if (MirrorSyncTable.BATCH_MOVEMENT in requestedTables) {
                    put(
                        MirrorSyncTable.BATCH_MOVEMENT,
                        db.batchMovementDao.getAll().map { it.toMirrorRow(db) },
                    )
                }
                if (MirrorSyncTable.REMINDER in requestedTables) {
                    put(
                        MirrorSyncTable.REMINDER,
                        db.reminderDao.getAll().map { it.toMirrorRow(db) },
                    )
                }
                if (MirrorSyncTable.EXPENSE in requestedTables) {
                    put(
                        MirrorSyncTable.EXPENSE,
                        db.expenseDao.getAll().map { it.toMirrorRow(db) },
                    )
                }
                if (MirrorSyncTable.BUY in requestedTables) {
                    put(
                        MirrorSyncTable.BUY,
                        db.buyDao.getAll().map { it.toMirrorRow(db) },
                    )
                }
                if (MirrorSyncTable.SALE in requestedTables) {
                    put(
                        MirrorSyncTable.SALE,
                        db.saleDao.getAll().map { it.toMirrorRow(db) },
                    )
                }
                if (MirrorSyncTable.FILE in requestedTables) {
                    put(
                        MirrorSyncTable.FILE,
                        db.fileDao.getAllFiles().map { it.toMirrorRow(db) },
                    )
                }
            },
        )
    }
}
