package ru.pavlig43.immutable.internal.component.items.declaration

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.immutable.api.component.DeclarationImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec


internal class DeclarationTableComponent(
    componentContext: ComponentContext,
    tableBuilder: DeclarationImmutableTableBuilder,
    onCreate: () -> Unit,
    onItemClick: (DeclarationTableUi) -> Unit,
    repository: ImmutableListRepository<Declaration>,
) : ImmutableTableComponent<Declaration, DeclarationTableUi, DeclarationField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = DeclarationFilterMatcher,
    sortMatcher = DeclarationSorter,
    repository = repository,
) {

    override val columns: ImmutableList<ColumnSpec<DeclarationTableUi, DeclarationField, TableData<DeclarationTableUi>>> =
        createDeclarationColumn(onCreate, ::onEvent)

}
private fun Declaration.toUi(): DeclarationTableUi {
    return DeclarationTableUi(
        composeId = id,
        vendorId = vendorId,
        vendorName = vendorName,
        bestBefore = bestBefore,
        displayName = displayName,
        createdAt = bestBefore,

        )

}