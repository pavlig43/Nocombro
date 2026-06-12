package ru.pavlig43.database

import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DatabaseMigration6To7Test : FunSpec({

    test("migration 6 to 7 removes the legacy sync change table") {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execSQL(
                """
                CREATE TABLE sync_change (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    entity_table TEXT NOT NULL
                )
                """.trimIndent()
            )
            connection.execSQL(
                """
                CREATE TABLE sync_state (
                    scope TEXT NOT NULL PRIMARY KEY,
                    device_id TEXT NOT NULL,
                    last_pull_at TEXT,
                    last_push_at TEXT,
                    last_remote_cursor TEXT
                )
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO sync_state VALUES (
                    'default',
                    'device-1',
                    '2026-06-10T10:00:00',
                    '2026-06-11T11:00:00',
                    'legacy-cursor'
                )
                """.trimIndent()
            )

            MIGRATION_6_7.migrate(connection)

            connection.prepare(
                """
                SELECT name
                FROM sqlite_master
                WHERE type = 'table' AND name = 'sync_change'
                """.trimIndent()
            ).use { statement ->
                statement.step() shouldBe false
            }
            connection.prepare("PRAGMA table_info(sync_state)").use { statement ->
                val columns = buildList {
                    while (statement.step()) {
                        add(statement.getText(1))
                    }
                }
                columns shouldBe listOf("scope", "last_pull_at", "last_push_at")
            }
            connection.prepare(
                """
                SELECT scope, last_pull_at, last_push_at
                FROM sync_state
                """.trimIndent()
            ).use { statement ->
                statement.step() shouldBe true
                statement.getText(0) shouldBe "default"
                statement.getText(1) shouldBe "2026-06-10T10:00:00"
                statement.getText(2) shouldBe "2026-06-11T11:00:00"
                statement.step() shouldBe false
            }
        } finally {
            connection.close()
        }
    }
})
