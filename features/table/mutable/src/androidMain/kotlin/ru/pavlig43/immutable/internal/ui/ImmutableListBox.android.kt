package ru.pavlig43.immutable.internal.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable

@Composable
internal actual fun BoxScope.ScrollBar(
    verticalState: LazyListState,
    horizontalState: ScrollState
) {
}