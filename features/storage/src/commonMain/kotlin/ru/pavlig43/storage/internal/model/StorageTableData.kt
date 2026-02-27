package ru.pavlig43.storage.internal.model

import androidx.compose.runtime.Immutable

@Immutable
data class StorageTableData(
    val displayedProducts: List<StorageProductUi> = emptyList()
)
