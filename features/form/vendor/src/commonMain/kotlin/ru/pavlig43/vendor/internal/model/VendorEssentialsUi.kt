package ru.pavlig43.vendor.internal.model

import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

internal data class VendorEssentialsUi(
    val displayName: String = "",
    val comment: String = "",
    val id: Int = 0,
): ISingleLineTableUi
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