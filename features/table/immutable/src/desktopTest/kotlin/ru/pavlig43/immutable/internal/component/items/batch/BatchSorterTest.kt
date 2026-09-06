package ru.pavlig43.immutable.internal.component.items.batch

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.DecimalData3
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

class BatchSorterTest : FunSpec({
    val oldest = batch(composeId = 1, dateBorn = LocalDate(2024, 1, 10))
    val middle = batch(composeId = 2, dateBorn = LocalDate(2025, 2, 20))
    val newest = batch(composeId = 3, dateBorn = LocalDate(2026, 3, 30))
    val batches = listOf(middle, newest, oldest)

    test("batches initially sort from oldest production date") {
        BatchSorter.sort(batches, sort = null)
            .map { it.composeId }
            .shouldContainExactly(1, 2, 3)
    }

    test("production date header sorts ascending and descending") {
        BatchSorter.sort(
            batches,
            SortState(BatchField.DATE_BORN, SortOrder.ASCENDING),
        ).map { it.composeId }.shouldContainExactly(1, 2, 3)

        BatchSorter.sort(
            batches,
            SortState(BatchField.DATE_BORN, SortOrder.DESCENDING),
        ).map { it.composeId }.shouldContainExactly(3, 2, 1)
    }

    test("resetting sort restores the initial production date order") {
        val sortedDescending = BatchSorter.sort(
            batches,
            SortState(BatchField.DATE_BORN, SortOrder.DESCENDING),
        )

        BatchSorter.sort(sortedDescending, sort = null)
            .map { it.composeId }
            .shouldContainExactly(1, 2, 3)
    }
})

private fun batch(
    composeId: Int,
    dateBorn: LocalDate,
): BatchTableUi = BatchTableUi(
    composeId = composeId,
    batchId = composeId,
    balance = DecimalData3(1),
    productName = "Product $composeId",
    vendorName = "Vendor $composeId",
    dateBorn = dateBorn,
)
