package ru.pavlig43.vendor.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.form.api.data.IUpdateRepository

internal class VendorInformationTabSlot(
    componentContext: ComponentContext,
    vendorId: Int,
//    updateRepository: IUpdateRepository<Vendor>
)  :VendorTabSlot {

    override val title: String = "Основная информация"
    override suspend fun onUpdate() {

    }
}