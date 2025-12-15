package ru.pavlig43.itemlist.statik.internal.ui

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
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingScreen
import ru.pavlig43.itemlist.core.data.IItemUi
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ru.pavlig43.itemlist.statik.internal.component.DocumentsStaticListContainer
import ru.pavlig43.itemlist.statik.internal.component.ItemListState
import ru.pavlig43.itemlist.statik.internal.component.StaticListComponent
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.DefaultTableCustomization
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableGroupContext
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.format.rememberCustomization
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns

enum class DocumentField {
    Id,
    Name,
    Type,
    CreatedAt,
    Comment
}

val columns: ImmutableList<ColumnSpec<DocumentItemUi, DocumentField, Unit>> =
    tableColumns<DocumentItemUi, DocumentField, Unit> {


        column(DocumentField.Id, valueOf = { it.id }) {
            header("Ид")
            cell { document, _ -> Text(document.id.toString()) }
            sortable()
            // Enable built‑in Text filter UI in header
            filter(TableFilterType.TextTableFilter())
            // Auto‑fit to content with optional max cap
            autoWidth(max = 500.dp)

        }

        column(DocumentField.Name, valueOf = { it.displayName }) {
            header("Название")
            cell { document, _ -> Text(document.displayName) }
            sortable()
        }
        column(DocumentField.Type, valueOf = { it.type }) {
            header("Тип")
            cell { document, _ -> Text(document.type.displayName) }
        }
        column(DocumentField.CreatedAt, valueOf = { it.createdAt }) {
            header("Создан")
            cell { document, _ ->
                Text(
                    document.createdAt.convertToDateOrDateTimeString(
                        DateFieldKind.Date
                    )
                )
            }
            sortable()
        }
        column(DocumentField.Comment, valueOf = { it.comment }) {
            header("Комментарий")
            cell { document, _ -> Text(document.comment) }
        }
    }

@OptIn(ExperimentalTableApi::class)
@Composable
internal fun DocScreen(
    component: DocumentsStaticListContainer,
    modifier: Modifier = Modifier
) {
    ImmutableListScreen(
        component = component.staticListComponent,
        columns = columns,
        onItemClick = { component.onItemClick(it) },
        tableData = Unit,
        modifier = modifier
    )

}

@OptIn(ExperimentalTableApi::class)
@Composable
internal fun <I : IItemUi, C, E> ImmutableListScreen(
    component: StaticListComponent<out GenericItem, I>,
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    onItemClick: (I) -> Unit,
    tableData: E,
    modifier: Modifier = Modifier
) {
    val itemListState: ItemListState<I> by component.itemListState.collectAsState()
    Box(modifier.padding(16.dp)) {

        when (val state = itemListState) {

            is ItemListState.Error<*> -> ErrorScreen(state.message)
            is ItemListState.Initial -> LoadingScreen()
            is ItemListState.Loading -> LoadingScreen()
            is ItemListState.Success -> {
                ImmutableListTable(
                    columns = columns,
                    items = state.data,
                    onRowClick = onItemClick,
                    tableData = tableData
                )
            }

        }
    }
}


@OptIn(ExperimentalTableApi::class)
@Composable
internal fun <I : Any, C, E> ImmutableListTable(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    items: List<I>,
    onRowClick: (I) -> Unit,
    tableData: E
) {
    val state = rememberTableState(
        columns = columns.map { it.key }.toImmutableList(),
        settings = TableSettings(
            stripedRows = true,
            showActiveFiltersHeader = true,
            selectionMode = SelectionMode.Single,
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

