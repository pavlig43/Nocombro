package ru.pavlig43.database

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DatabaseMigration7To8Test : FunSpec({

    test("migration 7 to 8 adds created at and allows several entries for one date") {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execSQL(
                """
                CREATE TABLE experiment_entry (
                    experiment_id INTEGER NOT NULL,
                    entry_date TEXT NOT NULL,
                    content TEXT NOT NULL,
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sync_id TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    deleted_at TEXT
                )
                """.trimIndent()
            )
            connection.execSQL(
                """
                CREATE UNIQUE INDEX index_experiment_entry_sync_id
                ON experiment_entry(sync_id)
                """.trimIndent()
            )
            connection.execSQL(
                """
                CREATE INDEX index_experiment_entry_experiment_id
                ON experiment_entry(experiment_id)
                """.trimIndent()
            )
            connection.execSQL(
                """
                CREATE UNIQUE INDEX index_experiment_entry_experiment_id_entry_date
                ON experiment_entry(experiment_id, entry_date)
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO experiment_entry (
                    experiment_id,
                    entry_date,
                    content,
                    sync_id,
                    updated_at,
                    deleted_at
                ) VALUES (
                    10,
                    '2026-06-30',
                    'first',
                    'entry-1',
                    '2026-06-30T12:00:00',
                    NULL
                )
                """.trimIndent()
            )

            MIGRATION_7_8.migrate(connection)

            connection.prepare("PRAGMA index_list(experiment_entry)").use { statement ->
                val indexes = buildList {
                    while (statement.step()) {
                        add(statement.getText(1) to statement.getLong(2))
                    }
                }
                indexes.contains("index_experiment_entry_experiment_id_entry_date" to 1L) shouldBe false
                indexes.contains("index_experiment_entry_sync_id" to 1L) shouldBe true
            }
            connection.prepare(
                """
                SELECT created_at
                FROM experiment_entry
                WHERE sync_id = 'entry-1'
                """.trimIndent()
            ).use { statement ->
                statement.step() shouldBe true
                statement.getText(0) shouldBe "2026-06-30T00:00:00"
            }

            connection.execSQL(
                """
                INSERT INTO experiment_entry (
                    experiment_id,
                    entry_date,
                    created_at,
                    content,
                    sync_id,
                    updated_at,
                    deleted_at
                ) VALUES (
                    10,
                    '2026-06-30',
                    '2026-06-30T14:00:00',
                    'second',
                    'entry-2',
                    '2026-06-30T14:00:00',
                    NULL
                )
                """.trimIndent()
            )
            connection.prepare(
                """
                SELECT COUNT(*)
                FROM experiment_entry
                WHERE experiment_id = 10 AND entry_date = '2026-06-30'
                """.trimIndent()
            ).use { statement ->
                statement.step() shouldBe true
                statement.getLong(0) shouldBe 2L
            }
        } finally {
            connection.close()
        }
    }
})
