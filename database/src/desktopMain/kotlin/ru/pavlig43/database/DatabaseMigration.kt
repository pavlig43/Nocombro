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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS experiment_reminder (
                experiment_id INTEGER NOT NULL,
                text TEXT NOT NULL,
                reminder_date_time TEXT NOT NULL,
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
            CREATE UNIQUE INDEX IF NOT EXISTS index_experiment_reminder_sync_id
            ON experiment_reminder(sync_id)
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_experiment_reminder_experiment_id
            ON experiment_reminder(experiment_id)
            """.trimIndent()
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE batch_cost_price_new (
                batch_id INTEGER NOT NULL,
                cost_price_per_unit INTEGER NOT NULL,
                sync_id TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT,
                PRIMARY KEY(batch_id),
                FOREIGN KEY(batch_id) REFERENCES batch(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO batch_cost_price_new (
                batch_id,
                cost_price_per_unit,
                sync_id,
                updated_at,
                deleted_at
            )
            SELECT
                batch_id,
                cost_price_per_unit,
                lower(
                    hex(randomblob(4)) || '-' ||
                    hex(randomblob(2)) || '-' ||
                    '4' || substr(hex(randomblob(2)), 2) || '-' ||
                    substr('89ab', abs(random()) % 4 + 1, 1) ||
                    substr(hex(randomblob(2)), 2) || '-' ||
                    hex(randomblob(6))
                ),
                strftime('%Y-%m-%dT%H:%M:%f', 'now'),
                NULL
            FROM batch_cost_price
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE batch_cost_price")
        connection.execSQL("ALTER TABLE batch_cost_price_new RENAME TO batch_cost_price")
        connection.execSQL(
            """
            CREATE UNIQUE INDEX index_batch_cost_price_sync_id
            ON batch_cost_price(sync_id)
            """.trimIndent()
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS mirror_deletion_journal (
                entity_table TEXT NOT NULL,
                sync_id TEXT NOT NULL,
                row_json TEXT NOT NULL,
                deleted_at TEXT NOT NULL,
                PRIMARY KEY(entity_table, sync_id)
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE batch_cost_price_new (
                batch_id INTEGER NOT NULL,
                cost_price_per_unit INTEGER NOT NULL,
                sync_id TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT,
                PRIMARY KEY(batch_id),
                FOREIGN KEY(batch_id) REFERENCES batch(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO batch_cost_price_new (
                batch_id,
                cost_price_per_unit,
                sync_id,
                updated_at,
                deleted_at
            )
            SELECT
                cost.batch_id,
                cost.cost_price_per_unit,
                batch.sync_id,
                cost.updated_at,
                cost.deleted_at
            FROM batch_cost_price AS cost
            INNER JOIN batch ON batch.id = cost.batch_id
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE batch_cost_price")
        connection.execSQL("ALTER TABLE batch_cost_price_new RENAME TO batch_cost_price")
        connection.execSQL(
            """
            CREATE UNIQUE INDEX index_batch_cost_price_sync_id
            ON batch_cost_price(sync_id)
            """.trimIndent()
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS sync_change")
        connection.execSQL(
            """
            CREATE TABLE sync_state_new (
                scope TEXT NOT NULL,
                last_pull_at TEXT,
                last_push_at TEXT,
                PRIMARY KEY(scope)
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO sync_state_new (
                scope,
                last_pull_at,
                last_push_at
            )
            SELECT
                scope,
                last_pull_at,
                last_push_at
            FROM sync_state
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE sync_state")
        connection.execSQL("ALTER TABLE sync_state_new RENAME TO sync_state")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE experiment_entry_new (
                experiment_id INTEGER NOT NULL,
                entry_date TEXT NOT NULL,
                created_at TEXT NOT NULL,
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
            INSERT INTO experiment_entry_new (
                experiment_id,
                entry_date,
                created_at,
                content,
                id,
                sync_id,
                updated_at,
                deleted_at
            )
            SELECT
                experiment_id,
                entry_date,
                entry_date || 'T00:00:00',
                content,
                id,
                sync_id,
                updated_at,
                deleted_at
            FROM experiment_entry
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE experiment_entry")
        connection.execSQL("ALTER TABLE experiment_entry_new RENAME TO experiment_entry")
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
    }
}
