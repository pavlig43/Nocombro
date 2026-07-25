package ru.pavlig43.database

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

class DatabaseMigration8To9Test : FunSpec({

    test("migration 8 to 9 adds money tables keys and indexes without changing old data") {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execSQL("PRAGMA foreign_keys = ON")
            connection.execSQL(
                """
                CREATE TABLE vendor (
                    display_name TEXT NOT NULL,
                    comment TEXT NOT NULL,
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sync_id TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    deleted_at TEXT
                )
                """.trimIndent()
            )
            connection.execSQL(
                """
                CREATE UNIQUE INDEX index_vendor_sync_id
                ON vendor(sync_id)
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO vendor (
                    display_name,
                    comment,
                    sync_id,
                    updated_at,
                    deleted_at
                ) VALUES (
                    'Старый поставщик',
                    'Данные схемы v8',
                    'vendor-v8',
                    '2026-07-01T10:00:00',
                    NULL
                )
                """.trimIndent()
            )

            MIGRATION_8_9.migrate(connection)

            val tables = connection.prepare(
                "SELECT name FROM sqlite_master WHERE type = 'table'"
            ).use { statement ->
                buildList {
                    while (statement.step()) add(statement.getText(0))
                }
            }
            tables shouldContainAll listOf("vendor", "money_account", "money_movement")

            connection.prepare(
                "SELECT display_name, comment FROM vendor WHERE sync_id = 'vendor-v8'"
            ).use { statement ->
                statement.step() shouldBe true
                statement.getText(0) shouldBe "Старый поставщик"
                statement.getText(1) shouldBe "Данные схемы v8"
            }

            val accountIndexes = loadIndexNames(connection, "money_account")
            accountIndexes shouldContainAll listOf("index_money_account_sync_id")
            val movementIndexes = loadIndexNames(connection, "money_movement")
            movementIndexes shouldContainAll listOf(
                "index_money_movement_sync_id",
                "index_money_movement_from_account_id",
                "index_money_movement_to_account_id",
                "index_money_movement_occurred_at",
            )

            val movementForeignKeys = connection.prepare(
                "PRAGMA foreign_key_list(money_movement)"
            ).use { statement ->
                buildList {
                    while (statement.step()) {
                        add(Triple(statement.getText(2), statement.getText(3), statement.getText(6)))
                    }
                }
            }
            movementForeignKeys shouldContainAll listOf(
                Triple("money_account", "from_account_id", "RESTRICT"),
                Triple("money_account", "to_account_id", "RESTRICT"),
            )
        } finally {
            connection.close()
        }
    }
})

private fun loadIndexNames(
    connection: androidx.sqlite.SQLiteConnection,
    tableName: String,
): List<String> = connection.prepare("PRAGMA index_list($tableName)").use { statement ->
    buildList {
        while (statement.step()) add(statement.getText(1))
    }
}
