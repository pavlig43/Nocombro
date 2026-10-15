package ru.pavlig43.transaction.internal.multiopzs

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import kotlin.time.ExperimentalTime

internal data class MultiOpzsRow(
    override val composeId: Int = 0,
    val syncId: String = defaultSyncId(),
    val productId: Int = 0,
    val productName: String = "",
    val declarationId: Int = 0,
    val declarationName: String = "",
    val vendorName: String = "",
    val count: DecimalData3 = DecimalData3(0),
    val loading: Boolean = false,
    val result: OpzsRowResult = OpzsRowResult.Empty,
) : IMultiLineTableUi

internal sealed interface OpzsRowResult {
    data object Empty : OpzsRowResult
    data object Waiting : OpzsRowResult
    data object Creating : OpzsRowResult
    data class Created(val transactionId: Int) : OpzsRowResult
    data class Failed(val reason: String) : OpzsRowResult
}

@OptIn(ExperimentalTime::class)
internal data class MultiOpzsState(
    val producedAt: LocalDateTime = getCurrentLocalDateTime(),
    val rows: List<MultiOpzsRow> = listOf(MultiOpzsRow()),
    val selectedIds: Set<Int> = emptySet(),
    val running: Boolean = false,
    val finished: Boolean = false,
) {
    val createdCount: Int get() = rows.count { it.result is OpzsRowResult.Created }

    val validationErrors: List<String> get() = rows.flatMap { row ->
        if (row.result is OpzsRowResult.Created) emptyList() else buildList {
            val prefix = "Строка ${row.composeId + 1}: "
            if (row.productId == 0) add(prefix + "Выберите продукт")
            if (row.declarationId == 0 && !row.loading) add(prefix + "Выберите декларацию")
            if (row.count.value <= 0) add(prefix + "Вес должен быть больше нуля")
        }
    }

    val errorMessages: List<String> get() = (validationErrors + rows.mapNotNull { row ->
        (row.result as? OpzsRowResult.Failed)?.let { "Строка ${row.composeId + 1}: ${it.reason}" }
    }).distinct()
}
