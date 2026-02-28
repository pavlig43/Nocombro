package ru.pavlig43.immutable.internal.component.items.storage

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.storage.StorageProduct
import ru.pavlig43.immutable.api.component.StorageImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

internal class ProductStorageComponent(
    componentContext: ComponentContext,
    tableBuilder: StorageImmutableTableBuilder,
    onCreate: () -> Unit,
    onItemClick: (StorageProductUi) -> Unit,
    repository: ImmutableListRepository<StorageProduct>,
) : ImmutableTableComponent<StorageProduct, StorageProductUi, StorageProductField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { toUi(expanded = false) }, // TODO: support expand/collapse
    filterMatcher = StorageFilterMatcher,
    sortMatcher = StorageSorter,
    repository = repository
) {
    override val columns: ImmutableList<ColumnSpec<StorageProductUi, StorageProductField, TableData<StorageProductUi>>>
        get() = createStorageColumns(
            onToggleExpand = { /* TODO: implement expand/collapse */ }
        )
}
