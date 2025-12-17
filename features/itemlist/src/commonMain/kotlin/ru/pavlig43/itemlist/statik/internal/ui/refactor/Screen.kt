package ru.pavlig43.itemlist.statik.internal.ui.refactor

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.core.DateFieldKind
import ru.pavlig43.core.convertToDateOrDateTimeString
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingScreen
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.DefaultTableCustomization
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns


enum class DocumentField {

    SELECTION,

    ID,
    NAME,
    TYPE,
    CREATED_AT,
    COMMENT
}
fun createColumn(
    onEvent: (SampleUiEvent) -> Unit,
): ImmutableList<ColumnSpec<DocumentItemUi, DocumentField, TableData<DocumentItemUi>>> {
    val columns =
        tableColumns<DocumentItemUi, DocumentField, TableData<DocumentItemUi>> {
            column(DocumentField.SELECTION, valueOf = {it.id}){
                title { "" }
                width(48.dp)
                resizable(false)
                cell { doc,tableData->
                    if (tableData.selectionModeEnabled){
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Checkbox(
                                checked = doc.id in tableData.selectedIds,
                                onCheckedChange = {
                                    onEvent(SampleUiEvent.ToggleSelection(doc.id))
                                },
                            )
                        }
                    }

                }

            }

            column(DocumentField.ID, valueOf = { it.id }) {
                header("Ид")
                cell { document, _ -> Text(document.id.toString()) }
                sortable()
                // Enable built‑in Text filter UI in header
                filter(TableFilterType.TextTableFilter())
                // Auto‑fit to content with optional max cap
                autoWidth(max = 500.dp)

            }

            column(DocumentField.NAME, valueOf = { it.displayName }) {
                header("Название")
                cell { document, _ -> Text(document.displayName) }
                sortable()
            }
            column(DocumentField.TYPE, valueOf = { it.type }) {
                header("Тип")
                cell { document, _ -> Text(document.type.displayName) }
            }
            column(DocumentField.CREATED_AT, valueOf = { it.createdAt }) {
                header("Создан")
                cell { document, _ ->
                    Text(
                        document.createdAt.toString()
                    )
                }
                sortable()
            }
            column(DocumentField.COMMENT, valueOf = { it.comment }) {
                header("Комментарий")
                cell { document, _ -> Text(document.comment) }
            }
        }
    return columns

}

@OptIn(ExperimentalTableApi::class)
@Composable
internal fun DocScreen1(
    component: DocumentTableComponent,
    modifier: Modifier = Modifier
) {

    ImmutableListScreen1(
        component = component,
        columns = createColumn(component::onEvent),
        onItemClick = { component.onItemClick(it) },
        modifier = modifier
    )

}

@OptIn(ExperimentalTableApi::class)
@Composable
internal fun <I : Any, C> ImmutableListScreen1(
    component: IImmutableTableComponent<I>,
    columns: ImmutableList<ColumnSpec<I, C, TableData<I>>>,
    onItemClick: (I) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemListState by component.itemListState.collectAsState()

    val tableData: TableData<I> by component.tableData.collectAsState()
    Box(modifier.padding(16.dp)) {

        when (val state = itemListState) {

            is ItemListState1.Error -> ErrorScreen(state.message)
            is ItemListState1.Loading -> LoadingScreen()
            is ItemListState1.Success -> {
                ImmutableListTable(
                    columns = columns,
                    items = tableData.displayedItems,
                    onRowClick = onItemClick,
                    tableData = tableData
                )
            }

        }
    }
}


@OptIn(ExperimentalTableApi::class)
@Composable
private fun <I : Any, C, E: TableData<I>> ImmutableListTable(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    items: List<I>,
    onRowClick: (I) -> Unit,
    tableData: E
) {
    val state = rememberTableState(
        columns = columns.map { it.key }.toImmutableList(),
        settings = TableSettings(
            stripedRows = true,
            autoApplyFilters = true,
            showFastFilters = true,
            showActiveFiltersHeader = true,
            selectionMode = SelectionMode.Multiple,
            pinnedColumnsCount = 1
        )
    )
    val verticalState = rememberLazyListState()
    val horizontalState = rememberScrollState()
    Box {

        Table(
            itemsCount = items.size,
            itemAt = { index -> items.getOrNull(index) },
            state = state,
            customization = DefaultTableCustomization(),
            tableData = tableData,
            columns = columns,
            verticalState = verticalState,
            horizontalState = horizontalState,
            onRowClick = onRowClick,
            modifier = Modifier.fillMaxSize()
                .padding(end = 16.dp, bottom = 16.dp)
        )

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

}