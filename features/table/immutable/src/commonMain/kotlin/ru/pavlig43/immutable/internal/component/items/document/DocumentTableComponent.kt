package ru.pavlig43.immutable.internal.component.items.document

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.immutable.api.component.DocumentImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import kotlin.time.ExperimentalTime


internal class DocumentTableComponent(
    componentContext: ComponentContext,
    tableBuilder: DocumentImmutableTableBuilder,
    onCreate: () -> Unit,
    onItemClick: (DocumentTableUi) -> Unit,
    repository: ImmutableListRepository<Document>,
) : ImmutableTableComponent<Document, DocumentTableUi, DocumentField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = DocumentFilterMatcher,
    sortMatcher = DocumentSorter,
    repository = repository
) {

    override val columns: ImmutableList<ColumnSpec<DocumentTableUi, DocumentField, TableData<DocumentTableUi>>> =
        createDocumentColumn(onCreate,tableBuilder.fullListDocumentTypes,::onEvent)

}


@OptIn(ExperimentalTime::class)
fun Document.toUi(): DocumentTableUi {

    return DocumentTableUi(
        composeId = id,
        displayName = displayName,
        type = type,
        createdAt = createdAt,
        comment = comment
    )
}

