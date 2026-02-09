package ru.pavlig43.vendor.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.create.component.CreateEssentialsComponent
import ru.pavlig43.create.data.CreateSingleItemRepository
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.vendor.internal.data.VendorEssentialsUi
import ru.pavlig43.vendor.internal.data.toDto

internal class CreateVendorComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    createRepository: CreateSingleItemRepository<Vendor>,
    componentFactory: EssentialComponentFactory<Vendor, VendorEssentialsUi>
) : CreateEssentialsComponent<Vendor, VendorEssentialsUi>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    createSingleItemRepository = createRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() },
)
