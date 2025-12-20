package ru.pavlig43.itemlist.internal.component.items.vendor

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.itemlist.api.component.VendorBuilder
import ru.pavlig43.itemlist.internal.component.ImmutableTableComponent
import ru.pavlig43.itemlist.internal.data.ImmutableListRepository
import ru.pavlig43.itemlist.internal.model.TableData
import ua.wwind.table.ColumnSpec


internal class VendorTableComponent(
    componentContext: ComponentContext,
    tableBuilder: VendorBuilder,
    onCreate: () -> Unit,
    onItemClick: (VendorItemUi) -> Unit,
    repository: ImmutableListRepository<Vendor>,
) : ImmutableTableComponent<Vendor, VendorItemUi, VendorField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = VendorFilterMatcher,
    sortMatcher = VendorSorter,
    repository = repository,
) {

    override val columns: ImmutableList<ColumnSpec<VendorItemUi, VendorField, TableData<VendorItemUi>>> =
        createVendorColumn(::onEvent)

}
private fun Vendor.toUi(): VendorItemUi {
    return VendorItemUi(
        id = id,
        displayName = displayName,
        comment = comment
    )
}