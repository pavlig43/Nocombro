package ru.pavlig43.vendor.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.manageitem.api.data.CreateEssentialsRepository
import ru.pavlig43.manageitem.internal.component.CreateEssentialsComponent
import ru.pavlig43.manageitem.internal.component.EssentialComponentFactory
import ru.pavlig43.vendor.internal.data.VendorEssentialsUi
import ru.pavlig43.vendor.internal.data.toDto

internal class CreateVendorComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    createRepository: CreateEssentialsRepository<Vendor>,
    componentFactory: EssentialComponentFactory<Vendor, VendorEssentialsUi>
) : CreateEssentialsComponent<Vendor, VendorEssentialsUi>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    createEssentialsRepository = createRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() },
)
