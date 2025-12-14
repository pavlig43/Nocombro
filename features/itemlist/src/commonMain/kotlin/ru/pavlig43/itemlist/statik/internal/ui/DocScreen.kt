package ru.pavlig43.itemlist.statik.internal.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ru.pavlig43.itemlist.statik.internal.component.DocumentsStaticListContainer
import ru.pavlig43.itemlist.statik.internal.component.ItemListState
import ua.wwind.paging.core.PagingData
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns
import ua.wwind.table.paging.Table as PT
data class Person(val name: String, val age: Int)

enum class PersonField { Name, Age }
data class PersonTableData(
    val displayedPeople: List<Person> = emptyList(),
)

val columns = tableColumns<Person, PersonField, PersonTableData> {
    column(PersonField.Name, valueOf = { it.name }) {
        header("Name")
        cell { person, _ -> Text(person.name) }
        sortable()
        // Enable built‑in Text filter UI in header
        filter(TableFilterType.TextTableFilter())
        // Auto‑fit to content with optional max cap
        autoWidth(max = 500.dp)

        // Optional footer with access to table data
        footer { tableData ->
            Text("Total: ${tableData.displayedPeople.size}")
        }
    }

    column(PersonField.Age, valueOf = { it.age }) {
        header("Age")
        cell { person, _ -> Text(person.age.toString()) }
        sortable()
        align(Alignment.TopEnd)
        filter(
            TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate,
                rangeOptions = 0 to 120
            )
        )
    }
}
@OptIn(ExperimentalTableApi::class)
@Composable
internal fun DocScreen(
    component: DocumentsStaticListContainer,
    modifier: Modifier = Modifier
){
    val a: ItemListState<DocumentItemUi> by component.staticListComponent.itemListState.collectAsState()
    when(val state =a ){
        is ItemListState.Error<*> -> Box{}
        is ItemListState.Initial -> Box{}
        is ItemListState.Loading -> Box{}
        is ItemListState.Success<*> -> {
            val items = state.data
                .map {
                item->
                Person((item as DocumentItemUi).displayName, item.id)
            }
//            val a = PagingData()
            PeopleTable(items)

//            PeopleTableWithPaging(
//
//            )
        }
    }



}
@Composable
fun PeopleTableWithPaging(
    pagingData: PagingData<Person>,
    onPersonClick: (Person) -> Unit,
) {
    // Create table state
    val tableState = rememberTableState(
        columns = PersonField.entries.toImmutableList(),
        settings = TableSettings(
            stripedRows = true,
        )
    )

    // Define columns
    val columns = tableColumns<Person, PersonField, Unit> {
        column(PersonField.Name, valueOf = { it.name }) {
            header("Name")
            cell { person, _ -> Text(person.name) }
            sortable()
        }
        column(PersonField.Age, valueOf = { it.age }) {
            header("Age")
            cell { person, _ -> Text(1.toString()) }
            sortable()
        }
    }

    // Scroll states for scrollbars
    val verticalState = rememberLazyListState()
    val horizontalState = rememberScrollState()

    // Render table with scrollbars
    Box {
        PT(
            items = pagingData, // Pass PagingData directly
            state = tableState,
            columns = columns,
            onRowClick = onPersonClick,
            verticalState = verticalState,
            horizontalState = horizontalState,
        )

        // Vertical scrollbar shows total dataset size
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(verticalState),
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // Horizontal scrollbar
        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(horizontalState),
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
@OptIn(ExperimentalTableApi::class)
@Composable
fun PeopleTable(items: List<Person>) {
    val a = PersonTableData(items)
    val state = rememberTableState(
        columns = columns.map { it.key }.toImmutableList(),
        settings = TableSettings(
            stripedRows = true,
            showActiveFiltersHeader = true,
            selectionMode = SelectionMode.Single,
        )
    )
    Table(
        itemsCount = items.size,
        itemAt = { index -> items.getOrNull(index) },
        state = state,
        tableData = a,
        columns = columns,
        onRowClick = { person -> println(person) },
    )
}

