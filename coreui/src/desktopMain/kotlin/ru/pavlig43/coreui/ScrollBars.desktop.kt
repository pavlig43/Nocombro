package ru.pavlig43.coreui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun BoxScope.ScrollBars(
    horizontalScrollState: ScrollState,
    verticalScrollState: LazyListState,
) {
    val scrollBarStyle = ScrollbarStyle(
        minimalHeight = 30.dp,
        thickness = 20.dp,
        shape = MaterialTheme.shapes.extraSmall,
        hoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        unhoverColor = MaterialTheme.colorScheme.onBackground,
        hoverDurationMillis = 300,
    )
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(horizontalScrollState),
        modifier = Modifier.Companion.fillMaxWidth().align(Alignment.Companion.BottomCenter),
        style = scrollBarStyle
    )
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(verticalScrollState),
        modifier = Modifier.Companion.fillMaxHeight().align(Alignment.Companion.CenterEnd),
        style = scrollBarStyle
    )
}