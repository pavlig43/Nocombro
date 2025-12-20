package ru.pavlig43.itemlist.internal.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal actual fun BoxScope.ScrollBar(
    verticalState: LazyListState,
    horizontalState: ScrollState
) {
    val lineColor = MaterialTheme.colorScheme.secondary
    val style = LocalScrollbarStyle.current.copy(
        unhoverColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
        hoverColor = MaterialTheme.colorScheme.onSecondary,
    )
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(verticalState),
        style = style,
        modifier = Modifier.align(Alignment.CenterEnd).padding(bottom = 24.dp)
            .background(lineColor)
    )

    // Horizontal scrollbar
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(horizontalState),
        style = style,
        modifier = Modifier.align(Alignment.BottomStart).padding(end = 24.dp)
            .background(lineColor)
    )
}