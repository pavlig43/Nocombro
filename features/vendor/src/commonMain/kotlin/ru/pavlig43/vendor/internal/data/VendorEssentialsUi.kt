package ru.pavlig43.vendor.internal.data

import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.manageitem.internal.data.ItemEssentialsUi

internal data class VendorEssentialsUi(
    val displayName: String = "",

    val comment:String ="",
    override val id: Int = 0,
): ItemEssentialsUi
internal fun Vendor.toUi(): VendorEssentialsUi {
    return VendorEssentialsUi(
        displayName = displayName,
        comment = comment,
        id = id
    )
}
internal fun VendorEssentialsUi.toDto(): Vendor {
    return Vendor(
        displayName = displayName,
        comment = comment,
        id = id
    )
}