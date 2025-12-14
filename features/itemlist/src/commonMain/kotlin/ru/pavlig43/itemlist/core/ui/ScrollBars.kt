package ru.pavlig43.itemlist.core.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seanproctor.datatable.DataTableState

@Composable
expect fun BoxScope.ScrollBars(
    tableState: DataTableState,
)


//
//@Composable
//actual fun BoxScope.ScrollBars(tableState: DataTableState) {
//}