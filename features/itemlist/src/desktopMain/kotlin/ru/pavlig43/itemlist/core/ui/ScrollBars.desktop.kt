package ru.pavlig43.itemlist.core.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seanproctor.datatable.DataTableScrollState
import com.seanproctor.datatable.DataTableState
import kotlin.math.roundToInt

@Composable
actual fun BoxScope.ScrollBars(tableState: DataTableState) {
    val scrollBarStyle = ScrollbarStyle(
        minimalHeight = 30.dp,
        thickness = 20.dp,
        shape = MaterialTheme.shapes.extraSmall,
        hoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        unhoverColor = MaterialTheme.colorScheme.onBackground,
        hoverDurationMillis = 300,
    )
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(tableState.horizontalScrollState),
        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
        style = scrollBarStyle
    )
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(tableState.verticalScrollState),
        modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
        style = scrollBarStyle
    )
}
@Composable
private fun rememberScrollbarAdapter(
    scrollState: DataTableScrollState
): androidx.compose.foundation.v2.ScrollbarAdapter = remember(scrollState) {
    DataTableScrollbarAdapter(scrollState)
}

private class DataTableScrollbarAdapter(
    private val scrollState: DataTableScrollState
) : androidx.compose.foundation.v2.ScrollbarAdapter {

    override val scrollOffset: Double
        get() = scrollState.offset.toDouble()

    override suspend fun scrollTo(scrollOffset: Double) {
        scrollState.scrollTo(scrollOffset.roundToInt())
    }

    override val contentSize: Double
        get() = scrollState.totalSize.toDouble()

    override val viewportSize: Double
        get() = scrollState.viewportSize.toDouble()
}