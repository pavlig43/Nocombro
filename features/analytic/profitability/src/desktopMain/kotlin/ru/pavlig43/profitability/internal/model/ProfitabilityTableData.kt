package ru.pavlig43.profitability.internal.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class ProfitabilityTableData(
    val displayedProducts: List<ProfitabilityUi> = emptyList()
)
