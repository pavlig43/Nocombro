package ru.pavlig43.coreui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable

@Composable
actual fun BoxScope.ScrollBars(
    horizontalScrollState: ScrollState,
    verticalScrollState: LazyListState
) {
}