package ru.pavlig43.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS experiment (
                title TEXT NOT NULL,
                idea_description TEXT NOT NULL,
                is_archived INTEGER NOT NULL,
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_experiment_sync_id
            ON experiment(sync_id)
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS experiment_entry (
                experiment_id INTEGER NOT NULL,
                entry_date TEXT NOT NULL,
                content TEXT NOT NULL,
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT,
                FOREIGN KEY(experiment_id) REFERENCES experiment(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_experiment_entry_sync_id
            ON experiment_entry(sync_id)
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_experiment_entry_experiment_id
            ON experiment_entry(experiment_id)
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_experiment_entry_experiment_id_entry_date
            ON experiment_entry(experiment_id, entry_date)
            """.trimIndent()
        )
    }
}
