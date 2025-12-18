package ru.pavlig43.itemlist.refactor

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.itemlist.refactor.manager.DeleteManager
import ru.pavlig43.itemlist.refactor.manager.FilterManager
import ru.pavlig43.itemlist.refactor.manager.SelectionManager
import ru.pavlig43.itemlist.refactor.manager.SortManager
import ru.pavlig43.itemlist.statik.ItemStaticListDependencies
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ru.pavlig43.itemlist.statik.internal.component.DocumentListRepository
import ru.pavlig43.itemlist.statik.internal.di.moduleFactory
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


class DocumentTableComponent(
    componentContext: ComponentContext,
    withCheckbox: Boolean,
    onCreate: () -> Unit,
    onItemClick: (DocumentItemUi) -> Unit,
    dependencies: ItemStaticListDependencies,
) : SlotComponent, ImmutableTableComponent<Document, DocumentItemUi, DocumentField>(
    componentContext = componentContext,
    withCheckbox = withCheckbox,
    onCreate = onCreate,
    onItemClick = onItemClick,

    mapper = { this.toUi() },
    filterMatcher = DocumentFilterMatcher,
    sortMatcher = DocumentSorter,
    dependencies = dependencies,
) {


    val a = MutableStateFlow(SlotComponent.TabModel(""))
    override val model: StateFlow<SlotComponent.TabModel> = a.asStateFlow()


}


@OptIn(ExperimentalTime::class)
fun Document.toUi(): DocumentItemUi {
    val date: LocalDate = Instant.fromEpochMilliseconds(createdAt)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    return DocumentItemUi(
        id = id,
        displayName = displayName,
        type = type,
        createdAt = date,
        comment = comment
    )
}

