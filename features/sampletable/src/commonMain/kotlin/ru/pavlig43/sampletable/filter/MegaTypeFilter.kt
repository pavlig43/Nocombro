package ru.pavlig43.sampletable.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.sampletable.model.MegaType
import ua.wwind.table.filter.data.*

/**
 * Creates a custom hierarchical filter for MegaType sealed interface.
 * Demonstrates custom filter with tree-like UI for nested sealed interfaces.
 */
fun createMegaTypeFilter(): TableFilterType.CustomTableFilter<MegaTypeFilterState, Unit> =
    TableFilterType.CustomTableFilter(
        renderFilter = MegaTypeFilterRenderer(),
        stateProvider = MegaTypeFilterStateProvider(),
    )

/**
 * Filter state representing selected MegaType values.
 */
@Immutable
data class MegaTypeFilterState(
    val selectedTypes: Set<MegaType> = emptySet(),
)

/**
 * Renderer for hierarchical MegaType filter with tree UI.
 */
private class MegaTypeFilterRenderer : CustomFilterRenderer<MegaTypeFilterState, Unit> {
    @Composable
    override fun RenderPanel(
        currentState: TableFilterState<MegaTypeFilterState>?,
        tableData: Unit,
        onDismiss: () -> Unit,
        onChange: (TableFilterState<MegaTypeFilterState>?) -> Unit,
    ): TableFilterType.CustomFilterActions {
        val current = currentState?.values?.firstOrNull() ?: MegaTypeFilterState()

        var selectedTypes by remember { mutableStateOf(current.selectedTypes) }

        // Auto-apply on change
        val applyFilter = {
            val newState = MegaTypeFilterState(selectedTypes = selectedTypes)
            onChange(
                TableFilterState(
                    constraint = FilterConstraint.EQUALS,
                    values = listOf(newState),
                ),
            )
        }

        Column(
            modifier = Modifier.width(300.dp).height(400.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Select Mega Types",
                style = MaterialTheme.typography.titleMedium,
            )

            HorizontalDivider()

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Type1 group (with sub-items)
                MegaTypeGroup(
                    title = "Type 1",
                    items = listOf(MegaType.Type1.PodType11, MegaType.Type1.PodType12),
                    selectedTypes = selectedTypes,
                    onToggle = { megaType ->
                        selectedTypes = if (megaType in selectedTypes) {
                            selectedTypes - megaType
                        } else {
                            selectedTypes + megaType
                        }
                        applyFilter()
                    },
                    onToggleGroup = { allSelected ->
                        selectedTypes = if (allSelected) {
                            selectedTypes - MegaType.Type1.PodType11 - MegaType.Type1.PodType12
                        } else {
                            selectedTypes + MegaType.Type1.PodType11 + MegaType.Type1.PodType12
                        }
                        applyFilter()
                    },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Type2 (single item)
        MegaTypeItem(
            title = "Type 2",
            megaType = MegaType.Type2,
            selected = MegaType.Type2 in selectedTypes,
            onToggle = {
                selectedTypes = if (MegaType.Type2 in selectedTypes) {
                    selectedTypes - MegaType.Type2
                } else {
                    selectedTypes + MegaType.Type2
                }
                applyFilter()
            },
        )
            }

            HorizontalDivider()

            // Selected count
            Text(
                "Selected: ${selectedTypes.size} of ${MegaType.entries.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        return remember {
            object : TableFilterType.CustomFilterActions {
                override fun applyFilter() {
                    val newState = MegaTypeFilterState(selectedTypes = selectedTypes)
                    onChange(
                        TableFilterState(
                            constraint = FilterConstraint.EQUALS,
                            values = listOf(newState),
                        ),
                    )
                }

                override fun clearFilter() {
                    selectedTypes = emptySet()
                    onChange(null)
                    onDismiss()
                }
            }
        }
    }

    @Composable
    override fun RenderFastFilter(
        currentState: TableFilterState<MegaTypeFilterState>?,
        tableData: Unit,
        onChange: (TableFilterState<MegaTypeFilterState>?) -> Unit,
    ) {
        val current = currentState?.values?.firstOrNull()
        val selectedCount = current?.selectedTypes?.size ?: 0

        val allSelected = selectedCount == MegaType.entries.size
        val someSelected = selectedCount > 0 && !allSelected

        // Tri-state checkbox for quick filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriStateCheckbox(
                state = when {
                    allSelected -> ToggleableState.On
                    someSelected -> ToggleableState.Indeterminate
                    else -> ToggleableState.Off
                },
                onClick = {
                    if (allSelected || someSelected) {
                        // Deselect all
                        onChange(null)
                    } else {
                        // Select all
                        val newState = MegaTypeFilterState(selectedTypes = MegaType.entries.toSet())
                        onChange(
                            TableFilterState(
                                constraint = FilterConstraint.EQUALS,
                                values = listOf(newState),
                            ),
                        )
                    }
                },
            )

            Text(
                "Mega Type ($selectedCount)",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * State provider for MegaType filter.
 */
private class MegaTypeFilterStateProvider : CustomFilterStateProvider<MegaTypeFilterState> {
    @Composable
    override fun buildChipText(state: TableFilterState<MegaTypeFilterState>?): String? {
        val selected = state?.values?.firstOrNull()?.selectedTypes
        val count = selected?.size ?: 0
        return if (count > 0) "MegaType: $count" else null
    }
}

/**
 * Hierarchical group item for Type1 with expandable sub-items.
 */
@Composable
private fun MegaTypeGroup(
    title: String,
    items: List<MegaType>,
    selectedTypes: Set<MegaType>,
    onToggle: (MegaType) -> Unit,
    onToggleGroup: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    val allSelected = items.all { it in selectedTypes }
    val someSelected = items.any { it in selectedTypes } && !allSelected

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Group header with tri-state checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Expand/Collapse icon
            Icon(
                imageVector = if (expanded) {
                    Icons.Default.ExpandLess
                } else {
                    Icons.Default.ExpandMore
                },
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(20.dp).clickable { expanded = !expanded },
            )

            // Tri-state checkbox for group
            TriStateCheckbox(
                state = when {
                    allSelected -> ToggleableState.On
                    someSelected -> ToggleableState.Indeterminate
                    else -> ToggleableState.Off
                },
                onClick = { onToggleGroup(allSelected || someSelected) },
            )

            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }

        // Sub-items (visible when expanded)
        if (expanded) {
            Column(
                modifier = Modifier.padding(start = 28.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items.forEach { item ->
                    MegaTypeItem(
                        title = when (item) {
                            is MegaType.Type1.PodType11 -> "Pod Type 1.1"
                            is MegaType.Type1.PodType12 -> "Pod Type 1.2"
                            else -> item.displayName
                        },
                        megaType = item,
                        selected = item in selectedTypes,
                        onToggle = { onToggle(item) },
                    )
                }
            }
        }
    }
}

/**
 * Individual MegaType item with checkbox.
 */
@Composable
private fun MegaTypeItem(
    title: String,
    megaType: MegaType,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(20.dp))

        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
        )

        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}
