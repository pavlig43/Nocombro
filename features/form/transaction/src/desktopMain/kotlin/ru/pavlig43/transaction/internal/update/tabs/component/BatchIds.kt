package ru.pavlig43.transaction.internal.update.tabs.component

internal fun <T> batchBalanceAdjustments(
    initialItems: Iterable<T>,
    currentItems: Iterable<T>,
    batchIdOf: (T) -> Int,
    countOf: (T) -> Long,
): Map<Int, Long> {
    val adjustments = mutableMapOf<Int, Long>()
    initialItems.addBatchCountsTo(adjustments, batchIdOf, countOf, sign = 1L)
    currentItems.addBatchCountsTo(adjustments, batchIdOf, countOf, sign = -1L)
    return adjustments.filterValues { it != 0L }
}

internal fun <T> Iterable<T>.duplicateBatchIds(batchIdOf: (T) -> Int): Set<Int> {
    val seen = mutableSetOf<Int>()
    return mapNotNullTo(mutableSetOf()) { item ->
        batchIdOf(item).takeIf { (it != 0) && !seen.add(it) }
    }
}

private fun <T> Iterable<T>.addBatchCountsTo(
    destination: MutableMap<Int, Long>,
    batchIdOf: (T) -> Int,
    countOf: (T) -> Long,
    sign: Long,
) {
    forEach { item ->
        val batchId = batchIdOf(item)
        if (batchId != 0) {
            destination[batchId] = destination.getOrDefault(batchId, 0L) + countOf(item) * sign
        }
    }
}
