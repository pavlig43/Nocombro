package ru.pavlig43.sampletable.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.EditableTable
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.DefaultStrings

@OptIn(ExperimentalTableApi::class)
@Composable
fun MainTable(
    state: TableState<ru.pavlig43.sampletable.column.PersonColumn>,
    tableData: ru.pavlig43.sampletable.model.PersonTableData,
    columns: ImmutableList<ColumnSpec<ru.pavlig43.sampletable.model.Person, ru.pavlig43.sampletable.column.PersonColumn, ru.pavlig43.sampletable.model.PersonTableData>>,
    customization: TableCustomization<ru.pavlig43.sampletable.model.Person, ru.pavlig43.sampletable.column.PersonColumn>,
    onFiltersChanged: (Map<ru.pavlig43.sampletable.column.PersonColumn, TableFilterState<*>>) -> Unit,
    onSortChanged: (SortState<ru.pavlig43.sampletable.column.PersonColumn>?) -> Unit,
    onRowEditStart: (ru.pavlig43.sampletable.model.Person, Int) -> Unit,
    onRowEditComplete: (Int) -> Boolean,
    onEditCancelled: (Int) -> Unit,
    useCompactMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Observe filters and sort state changes
    LaunchedEffect(state) {
        snapshotFlow { state.filters.toMap() }.collect { filters -> onFiltersChanged(filters) }
    }

    LaunchedEffect(state) { snapshotFlow { state.sort }.collect { sort -> onSortChanged(sort) } }

    EditableTable(
        itemsCount = tableData.displayedPeople.size,
        itemAt = { index -> tableData.displayedPeople.getOrNull(index) },
        state = state,
        tableData = tableData,
        columns = columns,
        customization = customization,
        strings = DefaultStrings,
        rowKey = { _, index -> index },
        rowEmbedded = { _, person ->
            val visible = person.expandedMovement
            if (visible) {
                HorizontalDivider(
                    thickness = state.dimensions.dividerThickness,
                    modifier = Modifier.width(state.tableWidth),
                )
            }
            AnimatedVisibility(
                visible = visible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                ru.pavlig43.sampletable.app.components.PersonMovementsSection(
                    person = person,
                    useCompactMode = useCompactMode
                )
            }
        },
        onRowEditStart = onRowEditStart,
        onRowEditComplete = onRowEditComplete,
        onEditCancelled = onEditCancelled,
        modifier = modifier,
    )
}
