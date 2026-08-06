package ru.pavlig43.nocombro.mobile.internal.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MOBILE_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(MOBILE_WAREHOUSE_PRODUCT_CREATE_SQL)
        connection.execSQL(MOBILE_WAREHOUSE_SNAPSHOT_CREATE_SQL)
    }
}

internal const val MOBILE_WAREHOUSE_PRODUCT_CREATE_SQL =
    """CREATE TABLE IF NOT EXISTS `mobile_warehouse_product` (
        `product_sync_id` TEXT NOT NULL,
        `display_name` TEXT NOT NULL,
        `main_balance` INTEGER NOT NULL,
        `experimental_balance` INTEGER NOT NULL,
        PRIMARY KEY(`product_sync_id`)
    )"""

internal const val MOBILE_WAREHOUSE_SNAPSHOT_CREATE_SQL =
    """CREATE TABLE IF NOT EXISTS `mobile_warehouse_snapshot` (
        `id` INTEGER NOT NULL,
        `updated_at` TEXT NOT NULL,
        PRIMARY KEY(`id`)
    )"""
