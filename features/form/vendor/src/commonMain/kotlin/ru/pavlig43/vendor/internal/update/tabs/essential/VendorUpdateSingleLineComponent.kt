package ru.pavlig43.vendor.internal.update.tabs.essential

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.vendor.internal.VendorField
import ru.pavlig43.vendor.internal.model.VendorEssentialsUi
import ru.pavlig43.vendor.internal.model.toDto
import ua.wwind.table.ColumnSpec

internal class VendorUpdateSingleLineComponent(
    componentContext: ComponentContext,
    vendorId: Int,
    updateRepository: UpdateSingleLineRepository<Vendor>,
    componentFactory: SingleLineComponentFactory<Vendor, VendorEssentialsUi>,
    observeOnItem: (VendorEssentialsUi) -> Unit,
    onSuccessInitData: (VendorEssentialsUi) -> Unit,
) : UpdateSingleLineComponent<Vendor, VendorEssentialsUi, VendorField>(
    componentContext = componentContext,
    id = vendorId,
    updateSingleLineRepository = updateRepository,
    componentFactory = componentFactory,
    observeOnItem = observeOnItem,
    onSuccessInitData = onSuccessInitData,
    mapperToDTO = { toDto() }
) {
    override val columns: ImmutableList<ColumnSpec<VendorEssentialsUi, VendorField, Unit>> =
        createVendorColumns1(
            onChangeItem = { item -> onChangeItem(item) }
        )

    override val errorMessages: Flow<List<String>> = errorTableMessages
}
