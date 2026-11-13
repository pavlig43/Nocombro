package ru.pavlig43.database

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DatabaseMigration10To11Test : FunSpec({
    test("existing products retain a null mixer limit and deleting a plan cascades to its rows") {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execSQL("PRAGMA foreign_keys = ON")
            connection.execSQL("CREATE TABLE product (id INTEGER PRIMARY KEY, display_name TEXT NOT NULL)")
            connection.execSQL("INSERT INTO product (id, display_name) VALUES (1, 'П/Ф')")
            MIGRATION_10_11.migrate(connection)
            connection.prepare("SELECT display_name, max_mix_grams FROM product WHERE id = 1").use {
                it.step() shouldBe true
                it.getText(0) shouldBe "П/Ф"
                it.isNull(1) shouldBe true
            }
            connection.execSQL("""
                INSERT INTO production_plan (id, title, created_at, sync_id, updated_at)
                VALUES (1, 'План', '2026-01-01T00:00:00', 'plan', '2026-01-01T00:00:00')
            """.trimIndent())
            connection.execSQL("""
                INSERT INTO production_plan_row (plan_id, product_id, position, planned_grams, package_grams, sync_id, updated_at)
                VALUES (1, 1, 0, 21000, 3000, 'row', '2026-01-01T00:00:00')
            """.trimIndent())
            connection.execSQL("DELETE FROM production_plan WHERE id = 1")
            connection.prepare("SELECT COUNT(*) FROM production_plan_row").use {
                it.step() shouldBe true
                it.getLong(0) shouldBe 0L
            }
        } finally {
            connection.close()
        }
    }
})
