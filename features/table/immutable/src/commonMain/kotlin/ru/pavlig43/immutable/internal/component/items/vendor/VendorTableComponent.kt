package ru.pavlig43.immutable.internal.component.items.vendor

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.immutable.api.component.VendorImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec


internal class VendorTableComponent(
    componentContext: ComponentContext,
    tableBuilder: VendorImmutableTableBuilder,
    onCreate: () -> Unit,
    onItemClick: (VendorTableUi) -> Unit,
    repository: ImmutableListRepository<Vendor>,
) : ImmutableTableComponent<Vendor, VendorTableUi, VendorField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = VendorFilterMatcher,
    sortMatcher = VendorSorter,
    repository = repository,
) {

    override val columns: ImmutableList<ColumnSpec<VendorTableUi, VendorField, TableData<VendorTableUi>>> =
        createVendorColumn(::onEvent)

}
private fun Vendor.toUi(): VendorTableUi {
    return VendorTableUi(
        composeId = id,
        displayName = displayName,
        comment = comment
    )
}