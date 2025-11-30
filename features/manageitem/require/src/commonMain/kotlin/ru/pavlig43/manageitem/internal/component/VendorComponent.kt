package ru.pavlig43.manageitem.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.manageitem.api.VendorFactoryParam
import ru.pavlig43.manageitem.api.data.CreateEssentialsRepository
import ru.pavlig43.manageitem.internal.data.VendorEssentialsUi
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class VendorComponent(
    componentContext: ComponentContext,
    param: VendorFactoryParam,
    createEssentialsRepository: CreateEssentialsRepository<Vendor>,
) : EssentialsComponent<Vendor, VendorEssentialsUi>(
    componentContext = componentContext,
    initItem = VendorEssentialsUi(),
    isValidValuesFactory = { displayName.isNotBlank() },
    mapperToDTO = {
        Vendor(
            displayName = displayName,
            comment = comment,
            id = id
        )
    },
    onSuccessUpsert = param.onSuccessUpsert,
    vendorInfoForTabName = { vendor -> param.onChangeValueForMainTab("*(Поставщик) ${vendor.displayName}") },
    upsertEssentialsRepository = createEssentialsRepository,
)
