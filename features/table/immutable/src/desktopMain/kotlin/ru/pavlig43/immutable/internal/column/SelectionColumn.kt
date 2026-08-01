package ru.pavlig43.immutable.internal.column

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.immutable.internal.ui.HighlightedTableText
import ru.pavlig43.immutable.internal.ui.LocalImmutableTableUiContext
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.ReadonlyTableColumnsBuilder

internal fun <T : IMultiLineTableUi, C, E : TableData<T>> ReadonlyTableColumnsBuilder<T, C, E>.idWithSelection(
    selectionKey: C,
    idKey: C,
    onEvent: (ImmutableTableUiEvent) -> Unit,
) {
    coreIdWithSelection(
        selectionKey = selectionKey,
        idKey = idKey,
        onSelectionUiEvent = { event ->
            onEvent(ImmutableTableUiEvent.Selection(event))
        },
    )
}

private fun <T : IMultiLineTableUi, C, E : TableData<T>>
    ReadonlyTableColumnsBuilder<T, C, E>.coreIdWithSelection(
    selectionKey: C,
    idKey: C,
    onSelectionUiEvent: (SelectionUiEvent) -> Unit,
) {
    column(selectionKey, { it.composeId }) {
        coreSelectionCell(onSelectionUiEvent)
    }
    column(idKey, valueOf = { it.composeId }) {
        header("Ид")
        align(Alignment.Center)
        cell { item, _ ->
            HighlightedTableText(
                text = item.composeId.toString(),
                modifier = Modifier.padding(horizontal = tableCellHorizontalPadding),
            )
        }
        autoWidth(max = 500.dp)
    }
}

private fun <T : IMultiLineTableUi, C, E : TableData<T>> ReadonlyColumnBuilder<T, C, E>.coreSelectionCell(
    onSelectionUiEvent: (SelectionUiEvent) -> Unit,
) {
    width(48.dp)
    autoWidth(48.dp)
    header {
        val context = LocalImmutableTableUiContext.current
        if (context.selectionEnabled) {
            val selectedCount = context.displayedIds.count { it in context.selectedIds }
            val state = when {
                selectedCount == 0 -> ToggleableState.Off
                selectedCount == context.displayedIds.size -> ToggleableState.On
                else -> ToggleableState.Indeterminate
            }
            TriStateCheckbox(
                state = state,
                enabled = context.displayedIds.isNotEmpty(),
                onClick = {
                    onSelectionUiEvent(
                        SelectionUiEvent.ToggleSelectAll(context.displayedIds),
                    )
                },
            )
        }
    }

    cell { item, tableData ->
        if (tableData.isSelectionMode) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Checkbox(
                    checked = item.composeId in tableData.selectedIds,
                    onCheckedChange = {
                        onSelectionUiEvent(
                            SelectionUiEvent.ToggleSelection(item.composeId),
                        )
                    },
                )
            }
        }
    }
}
