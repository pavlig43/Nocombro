package ru.pavlig43.tablecore.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.Dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.SortState
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState

/**
 * Keeps the complete table state in the current [androidx.compose.runtime.saveable.SaveableStateHolder].
 * Desktop saveable state is in-memory, so the same typed instance can be restored without serialization.
 */
@Composable
@Suppress("LongParameterList")
fun <C> rememberSaveableTableState(
    columns: ImmutableList<C>,
    initialSort: SortState<C>? = null,
    initialOrder: ImmutableList<C> = columns,
    initialWidths: ImmutableMap<C, Dp> = persistentMapOf(),
    settings: TableSettings = TableSettings(),
    dimensions: TableDimensions = TableDefaults.standardDimensions(),
): TableState<C> {
    val initialState = rememberTableState(
        columns = columns,
        initialSort = initialSort,
        initialOrder = initialOrder,
        initialWidths = initialWidths,
        settings = settings,
        dimensions = dimensions,
    )

    return rememberSaveable(
        columns,
        settings,
        dimensions,
        saver = tableStateSaver(),
    ) {
        initialState
    }
}

private fun <C> tableStateSaver(): Saver<TableState<C>, TableState<C>> = Saver(
    save = { it },
    restore = { it },
)
