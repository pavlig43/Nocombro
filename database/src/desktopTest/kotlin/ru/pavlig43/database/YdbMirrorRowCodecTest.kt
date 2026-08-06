package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import ru.pavlig43.database.data.sync.mirror.BatchCostPriceYdbMirrorCodec
import ru.pavlig43.database.data.sync.mirror.ExperimentYdbMirrorCodec
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.MoneyAccountYdbMirrorCodec
import ru.pavlig43.database.data.sync.mirror.MoneyMovementYdbMirrorCodec
import ru.pavlig43.database.data.sync.mirror.ProductYdbMirrorCodec
import ru.pavlig43.database.data.sync.mirror.TransactionYdbMirrorCodec
import ru.pavlig43.database.data.sync.mirror.VendorYdbMirrorCodec
import ru.pavlig43.database.data.sync.mirror.YdbMirrorJdbcConfig
import ru.pavlig43.database.data.sync.mirror.supportedYdbMirrorCodecs
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec

/** Проверяет SQL и типы колонок codec, включая атомарный условный `UPSERT`. */
class YdbMirrorRowCodecTest : DesktopMainDispatcherFunSpec({
    test("mirror table path uses optional root") {
        val config = YdbMirrorJdbcConfig(
            jdbcUrl = "jdbc:ydb:test",
            authToken = null,
            serviceAccountFile = null,
            tableRoot = "/nocombro/mirror/",
        )

        config.tablePath(MirrorSyncTable.VENDOR) shouldBe "nocombro/mirror/vendor"
    }

    test("jdbc config uses default url when setting is missing") {
        val config = YdbMirrorJdbcConfig.fromSettings(
            readSetting = { _, _ -> null },
            defaultServiceAccountFile = { null },
        )

        config.jdbcUrl shouldBe YdbMirrorJdbcConfig.DEFAULT_JDBC_URL
        config.serviceAccountFile shouldBe null
    }

    test("vendor codec builds typed ydb sql") {
        VendorYdbMirrorCodec.selectAllSql("vendor") shouldContain "display_name"
        VendorYdbMirrorCodec.upsertSql("vendor") shouldContain "CAST(? AS Utf8)"
    }

    test("conditional batch upsert compares versions and returns all final rows in one request") {
        val sql = VendorYdbMirrorCodec.conditionalBatchUpsertSql("vendor")

        sql shouldContain "DECLARE ${'$'}batch AS List<Struct<"
        sql shouldContain "p1: Utf8"
        sql shouldContain "p2: Utf8?"
        sql shouldContain "FROM AS_TABLE(${'$'}batch) AS incoming"
        sql shouldContain "LEFT JOIN `vendor` AS existing"
        sql shouldContain "existing.updated_at"
        sql shouldContain "incoming.p4"
        sql shouldContain "SELECT sync_id, display_name, comment, updated_at, deleted_at"
    }

    test("batch cost codec uses int64 cost") {
        BatchCostPriceYdbMirrorCodec.selectAllSql("batch_cost_price") shouldContain "cost_price_per_unit"
        BatchCostPriceYdbMirrorCodec.upsertSql("batch_cost_price") shouldContain
            "CAST(? AS Int64)"
    }

    test("initial jdbc gateway codecs are explicit") {
        supportedYdbMirrorCodecs.keys shouldBe MirrorSyncTable.mirroredBusinessTables.toSet()
    }

    test("root codecs use numeric and boolean parameter types") {
        ProductYdbMirrorCodec.upsertSql("product") shouldContain "CAST(? AS Int64)"
        ProductYdbMirrorCodec.upsertSql("product") shouldContain "CAST(? AS Int32)"
        TransactionYdbMirrorCodec.upsertSql("transact") shouldContain "CAST(? AS Bool)"
        ExperimentYdbMirrorCodec.upsertSql("experiment") shouldContain "CAST(? AS Bool)"
    }

    test("money codecs use sync ids for account relations") {
        MoneyAccountYdbMirrorCodec.upsertSql("money_account") shouldContain "CAST(? AS Bool)"
        MoneyMovementYdbMirrorCodec.upsertSql("money_movement") shouldContain "CAST(? AS Int64)"
        MoneyMovementYdbMirrorCodec.columnNames shouldBe listOf(
            "sync_id",
            "kind",
            "category",
            "amount",
            "occurred_at",
            "from_account_sync_id",
            "to_account_sync_id",
            "counterparty",
            "comment",
            "updated_at",
            "deleted_at",
        )
    }

    test("every mirror codec contains sync and version columns") {
        supportedYdbMirrorCodecs.values.forEach { codec ->
            codec.columnNames.first() shouldBe "sync_id"
            codec.columnNames.contains("updated_at") shouldBe true
            codec.columnNames.contains("deleted_at") shouldBe true
        }
    }
})
