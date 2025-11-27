package ru.pavlig43.vendor.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.form.api.component.UpdateItemComponent
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.vendor.internal.toVendor

//internal class VendorRequiresTabSlot(
//    componentContext: ComponentContext,
//    vendorId: Int,
//    updateRepository: IUpdateRepository<Vendor, Vendor>,
//    onChangeValueForMainTab: (String) -> Unit
//) : UpdateItemComponent<Vendor, VendorType>(
//    componentContext = componentContext,
//    id = vendorId,
//    typeVariantList = VendorType.entries,
//    updateRepository = updateRepository,
//    mapper = { toVendor() },
//    onChangeValueForMainTab = onChangeValueForMainTab
//), VendorTabSlot {
//
//    override val title: String = "Основная информация"
//}

