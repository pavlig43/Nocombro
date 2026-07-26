package ru.pavlig43.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE batch_movement ADD COLUMN storage_location TEXT NOT NULL DEFAULT 'MAIN'"
        )
        connection.execSQL(
            "ALTER TABLE transact ADD COLUMN stock_operation_reason TEXT"
        )
    }
}

