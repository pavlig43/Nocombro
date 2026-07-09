package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import ru.pavlig43.database.data.sync.mirror.BatchCostPriceYdbMirrorCodec
import ru.pavlig43.database.data.sync.mirror.ExperimentYdbMirrorCodec
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.ProductYdbMirrorCodec
import ru.pavlig43.database.data.sync.mirror.TransactionYdbMirrorCodec
import ru.pavlig43.database.data.sync.mirror.VendorYdbMirrorCodec
import ru.pavlig43.database.data.sync.mirror.YdbMirrorJdbcConfig
import ru.pavlig43.database.data.sync.mirror.supportedYdbMirrorCodecs
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec

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

    test("every mirror codec contains sync and version columns") {
        supportedYdbMirrorCodecs.values.forEach { codec ->
            codec.columnNames.first() shouldBe "sync_id"
            codec.columnNames.contains("updated_at") shouldBe true
            codec.columnNames.contains("deleted_at") shouldBe true
        }
    }
})
