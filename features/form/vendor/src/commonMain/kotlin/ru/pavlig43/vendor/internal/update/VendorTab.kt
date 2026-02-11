package ru.pavlig43.vendor.internal.update

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface VendorTab {

    @Serializable
    data object Essential : VendorTab

    @Serializable
    data object Files : VendorTab
}
