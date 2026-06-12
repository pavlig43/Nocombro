package ru.pavlig43.database

import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DatabaseMigration5To6Test : FunSpec({

    test("migration 5 to 6 replaces batch cost id with batch sync id without changing values") {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execSQL(
                """
                CREATE TABLE batch (
                    id INTEGER NOT NULL PRIMARY KEY,
                    sync_id TEXT NOT NULL
                )
                """.trimIndent()
            )
            connection.execSQL(
                """
                CREATE TABLE batch_cost_price (
                    batch_id INTEGER NOT NULL PRIMARY KEY,
                    cost_price_per_unit INTEGER NOT NULL,
                    sync_id TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    deleted_at TEXT
                )
                """.trimIndent()
            )
            connection.execSQL("INSERT INTO batch VALUES (7, 'batch-sync-id')")
            connection.execSQL(
                """
                INSERT INTO batch_cost_price VALUES (
                    7,
                    12345,
                    'old-random-cost-id',
                    '2026-06-01T10:11:12.123',
                    '2026-06-02T13:14:15.456'
                )
                """.trimIndent()
            )

            MIGRATION_5_6.migrate(connection)

            connection.prepare(
                """
                SELECT batch_id, cost_price_per_unit, sync_id, updated_at, deleted_at
                FROM batch_cost_price
                """.trimIndent()
            ).use { statement ->
                statement.step() shouldBe true
                statement.getLong(0) shouldBe 7L
                statement.getLong(1) shouldBe 12345L
                statement.getText(2) shouldBe "batch-sync-id"
                statement.getText(3) shouldBe "2026-06-01T10:11:12.123"
                statement.getText(4) shouldBe "2026-06-02T13:14:15.456"
                statement.step() shouldBe false
            }
        } finally {
            connection.close()
        }
    }
})
