package ru.pavlig43.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE product ADD COLUMN max_mix_grams INTEGER")
        connection.execSQL(
            """
            CREATE TABLE production_plan (
                title TEXT NOT NULL, created_at TEXT NOT NULL,
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sync_id TEXT NOT NULL,
                updated_at TEXT NOT NULL, deleted_at TEXT
            )
            """.trimIndent()
        )
        connection.execSQL("CREATE UNIQUE INDEX index_production_plan_sync_id ON production_plan(sync_id)")
        connection.execSQL(
            """
            CREATE TABLE production_plan_row (
                plan_id INTEGER NOT NULL, product_id INTEGER NOT NULL, position INTEGER NOT NULL,
                planned_grams INTEGER NOT NULL, package_grams INTEGER NOT NULL,
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sync_id TEXT NOT NULL,
                updated_at TEXT NOT NULL, deleted_at TEXT,
                FOREIGN KEY(plan_id) REFERENCES production_plan(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(product_id) REFERENCES product(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("CREATE INDEX index_production_plan_row_plan_id ON production_plan_row(plan_id)")
        connection.execSQL("CREATE INDEX index_production_plan_row_product_id ON production_plan_row(product_id)")
        connection.execSQL("CREATE UNIQUE INDEX index_production_plan_row_sync_id ON production_plan_row(sync_id)")
    }
}
