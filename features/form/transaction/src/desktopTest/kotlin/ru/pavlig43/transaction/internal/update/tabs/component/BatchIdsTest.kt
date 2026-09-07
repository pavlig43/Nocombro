package ru.pavlig43.transaction.internal.update.tabs.component

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class BatchIdsTest : FunSpec({
    val rows = listOf(
        Row(batchId = 0),
        Row(batchId = 3),
        Row(batchId = 5),
        Row(batchId = 3),
        Row(batchId = 5),
        Row(batchId = 5),
    )

    test("balance adjustments restore saved rows and subtract current rows") {
        batchBalanceAdjustments(
            initialItems = listOf(Row(batchId = 3, count = 10_000)),
            currentItems = listOf(
                Row(batchId = 3, count = 8_000),
                Row(batchId = 5, count = 4_000),
            ),
            batchIdOf = Row::batchId,
            countOf = Row::count,
        ) shouldBe mapOf(
            3 to 2_000L,
            5 to -4_000L,
        )
    }

    test("duplicate batch ids contain repeated nonzero ids") {
        rows.duplicateBatchIds(Row::batchId)
            .shouldContainExactlyInAnyOrder(3, 5)
    }

    test("zero batch id is neither adjusted nor duplicate") {
        listOf(Row(0), Row(0)).run {
            batchBalanceAdjustments(
                initialItems = this,
                currentItems = this,
                batchIdOf = Row::batchId,
                countOf = Row::count,
            ) shouldBe emptyMap()
            duplicateBatchIds(Row::batchId) shouldBe emptySet()
        }
    }
})

private data class Row(
    val batchId: Int,
    val count: Long = 1L,
)
