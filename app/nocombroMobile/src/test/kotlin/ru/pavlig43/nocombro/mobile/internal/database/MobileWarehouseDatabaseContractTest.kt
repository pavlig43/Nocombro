package ru.pavlig43.nocombro.mobile.internal.database

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MobileWarehouseDatabaseContractTest {
    @Test
    fun `migration only adds warehouse tables and keeps experiment tables intact`() {
        assertContains(MOBILE_WAREHOUSE_PRODUCT_CREATE_SQL, "mobile_warehouse_product")
        assertContains(MOBILE_WAREHOUSE_PRODUCT_CREATE_SQL, "product_sync_id")
        assertContains(MOBILE_WAREHOUSE_SNAPSHOT_CREATE_SQL, "mobile_warehouse_snapshot")
        assertContains(MOBILE_WAREHOUSE_SNAPSHOT_CREATE_SQL, "updated_at")
        assertFalse(MOBILE_WAREHOUSE_PRODUCT_CREATE_SQL.contains("DROP", ignoreCase = true))
        assertFalse(MOBILE_WAREHOUSE_SNAPSHOT_CREATE_SQL.contains("DROP", ignoreCase = true))
    }

    @Test
    fun `exported version two schema contains both old experiments and warehouse snapshot`() {
        val schema = locateSchema("2.json").readText()

        assertContains(schema, "\"version\": 2")
        assertContains(schema, "\"tableName\": \"experiment\"")
        assertContains(schema, "\"tableName\": \"experiment_entry\"")
        assertContains(schema, "\"tableName\": \"mobile_warehouse_product\"")
        assertContains(schema, "\"tableName\": \"mobile_warehouse_snapshot\"")
        assertTrue(locateSchema("1.json").isFile)
    }

    private fun locateSchema(name: String): File {
        val relative = "schemas/ru.pavlig43.nocombro.mobile.internal.database.NocombroMobileDatabase/$name"
        return sequenceOf(File(relative), File("app/nocombroMobile/$relative"))
            .first(File::isFile)
    }
}
