package ru.pavlig43.sampletable.filter

/** State for numeric range filter with visual distribution. */
data class NumericRangeFilterState(
    val min: Int,
    val max: Int,
    val showOutliers: Boolean = false,
)
