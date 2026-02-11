package ru.pavlig43.vendor.internal.component.tabs.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.update.component.UpdateEssentialsComponent
import ru.pavlig43.update.data.UpdateSingleLineRepository
import ru.pavlig43.vendor.internal.data.VendorEssentialsUi
import ru.pavlig43.vendor.internal.data.toDto

internal class VendorEssentialsComponent(
    componentContext: ComponentContext,
    vendorId: Int,
    updateRepository: UpdateSingleLineRepository<Vendor>,
    componentFactory: EssentialComponentFactory<Vendor, VendorEssentialsUi>,
) : UpdateEssentialsComponent<Vendor, VendorEssentialsUi>(
    componentContext = componentContext,
    id = vendorId,
    updateSingleLineRepository = updateRepository,
    componentFactory = componentFactory,
    mapperToDTO = {toDto()}
) {
    override val errorMessages: Flow<List<String>> = itemFields.map { vendor->
        buildList {
            if (vendor.displayName.isBlank()){
                add("Имя поставщика не должно быть пустым")
            }
        }

    }
}