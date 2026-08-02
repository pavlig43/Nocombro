package ru.pavlig43.nocombro.mobile.sync

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MobileYdbMirrorGatewayTest {
    @Test
    fun `successful snapshot reads every mobile table once`() {
        val reads = MobileMirrorTable.entries.associateWith { 0 }.toMutableMap()

        val rows = loadMobileMirrorTables(
            tables = MobileMirrorTable.entries,
            delayAction = {},
        ) { table ->
            reads[table] = reads.getValue(table) + 1
            emptyList()
        }

        assertEquals(MobileMirrorTable.entries.toSet(), rows.keys)
        assertEquals(setOf(1), reads.values.toSet())
    }

    @Test
    fun `conditional upsert supplies a row source required by YDB`() {
        val sql = mobileConditionalUpsertSql(
            columns = listOf(
                "sync_id",
                "title",
                "is_archived",
                "updated_at",
                "deleted_at",
            ),
            tablePath = "experiment",
        )

        assertContains(
            sql,
            "SELECT CAST(? AS Utf8), CAST(? AS Utf8), CAST(? AS Bool), " +
                "CAST(? AS Utf8), CAST(? AS Utf8)\n" +
                "FROM AS_TABLE(AsList(AsStruct(1 AS _source)))\n" +
                "WHERE NOT EXISTS",
        )
        assertEquals(8, sql.count { it == '?' })
    }
}
