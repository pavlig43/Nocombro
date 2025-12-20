package ru.pavlig43.itemlist.internal.component.items.declaration

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.itemlist.api.component.DeclarationBuilder
import ru.pavlig43.itemlist.internal.component.ImmutableTableComponent
import ru.pavlig43.itemlist.internal.data.ImmutableListRepository
import ru.pavlig43.itemlist.internal.model.TableData
import ua.wwind.table.ColumnSpec


internal class DeclarationTableComponent(
    componentContext: ComponentContext,
    tableBuilder: DeclarationBuilder,
    onCreate: () -> Unit,
    onItemClick: (DeclarationItemUi) -> Unit,
    repository: ImmutableListRepository<Declaration>,
) : ImmutableTableComponent<Declaration, DeclarationItemUi, DeclarationField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = DeclarationFilterMatcher,
    sortMatcher = DeclarationSorter,
    repository = repository,
) {

    override val columns: ImmutableList<ColumnSpec<DeclarationItemUi, DeclarationField, TableData<DeclarationItemUi>>> =
        createDeclarationColumn(onCreate,::onEvent)

}
private fun Declaration.toUi(): DeclarationItemUi {
    return DeclarationItemUi(
        id = id,
        vendorId = vendorId,
        vendorName = vendorName,
        bestBefore = bestBefore,
        displayName = displayName,
        createdAt = bestBefore,

        )

}