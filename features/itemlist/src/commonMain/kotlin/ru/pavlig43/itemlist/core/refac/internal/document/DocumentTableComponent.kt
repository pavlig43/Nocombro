package ru.pavlig43.itemlist.core.refac.internal.document

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.itemlist.core.refac.api.DocumentBuilder
import ru.pavlig43.itemlist.core.refac.core.component.ImmutableTableComponent
import ru.pavlig43.itemlist.core.refac.core.model.TableData
import ru.pavlig43.itemlist.statik.ItemStaticListDependencies
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ua.wwind.table.ColumnSpec
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


internal class DocumentTableComponent(
    componentContext: ComponentContext,
    tableBuilder: DocumentBuilder,
    onCreate: () -> Unit,
    onItemClick: (DocumentItemUi) -> Unit,
    dependencies: ItemStaticListDependencies,
) : ImmutableTableComponent<Document, DocumentItemUi, DocumentField>(
    componentContext = componentContext,
    tableBuilder1 = tableBuilder,
    dependencies = dependencies,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = DocumentFilterMatcher,
    sortMatcher = DocumentSorter,
) {

    override val columns: ImmutableList<ColumnSpec<DocumentItemUi, DocumentField, TableData<DocumentItemUi>>> =
        createDocumentColumn(tableBuilder.fullListDocumentTypes,::onEvent)

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

