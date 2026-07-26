package ru.pavlig43.database

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DatabaseMigration9To10Test : FunSpec({
    test("migration 9 to 10 keeps rows and assigns old movements to main storage") {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execSQL(
                """
                CREATE TABLE batch_movement (
                    batch_id INTEGER NOT NULL,
                    movement_type TEXT NOT NULL,
                    count INTEGER NOT NULL,
                    transaction_id INTEGER NOT NULL,
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sync_id TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    deleted_at TEXT
                )
                """.trimIndent()
            )
            connection.execSQL(
                """
                CREATE TABLE transact (
                    transaction_type TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    comment TEXT NOT NULL,
                    is_completed INTEGER NOT NULL,
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sync_id TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    deleted_at TEXT
                )
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO batch_movement (
                    batch_id, movement_type, count, transaction_id,
                    sync_id, updated_at, deleted_at
                ) VALUES (7, 'INCOMING', 12500, 3, 'movement-v9', '2026-07-01T10:00:00', NULL)
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO transact (
                    transaction_type, created_at, comment, is_completed,
                    sync_id, updated_at, deleted_at
                ) VALUES ('BUY', '2026-07-01T10:00:00', 'old', 1, 'transaction-v9', '2026-07-01T10:00:00', NULL)
                """.trimIndent()
            )

            MIGRATION_9_10.migrate(connection)

            connection.prepare(
                "SELECT batch_id, count, storage_location FROM batch_movement WHERE sync_id = 'movement-v9'"
            ).use { statement ->
                statement.step() shouldBe true
                statement.getLong(0) shouldBe 7L
                statement.getLong(1) shouldBe 12500L
                statement.getText(2) shouldBe "MAIN"
            }
            connection.prepare(
                "SELECT transaction_type, stock_operation_reason FROM transact WHERE sync_id = 'transaction-v9'"
            ).use { statement ->
                statement.step() shouldBe true
                statement.getText(0) shouldBe "BUY"
                statement.isNull(1) shouldBe true
            }
        } finally {
            connection.close()
        }
    }
})
