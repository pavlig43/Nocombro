package ru.pavlig43.immutable.internal.ui

import androidx.compose.runtime.compositionLocalOf

internal data class ImmutableTableUiContext(
    val searchQuery: String = "",
    val displayedIds: Set<Int> = emptySet(),
    val selectedIds: Set<Int> = emptySet(),
    val selectionEnabled: Boolean = false,
)

internal val LocalImmutableTableUiContext = compositionLocalOf { ImmutableTableUiContext() }
