package ru.pavlig43.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS money_account (
                name TEXT NOT NULL,
                account_type TEXT NOT NULL,
                opened_at TEXT NOT NULL,
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
            CREATE UNIQUE INDEX IF NOT EXISTS index_money_account_sync_id
            ON money_account(sync_id)
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS money_movement (
                kind TEXT NOT NULL,
                category TEXT,
                amount INTEGER NOT NULL,
                occurred_at TEXT NOT NULL,
                from_account_id INTEGER,
                to_account_id INTEGER,
                counterparty TEXT NOT NULL,
                comment TEXT NOT NULL,
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT,
                FOREIGN KEY(from_account_id) REFERENCES money_account(id) ON DELETE RESTRICT,
                FOREIGN KEY(to_account_id) REFERENCES money_account(id) ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_money_movement_sync_id
            ON money_movement(sync_id)
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_money_movement_from_account_id
            ON money_movement(from_account_id)
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_money_movement_to_account_id
            ON money_movement(to_account_id)
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_money_movement_occurred_at
            ON money_movement(occurred_at)
            """.trimIndent()
        )
    }
}
