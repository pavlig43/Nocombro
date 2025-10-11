package ru.pavlig43.vendor.internal.component

import kotlinx.serialization.Serializable


@Serializable
internal sealed interface VendorTab {
    @Serializable
    data object RequireValues: VendorTab

    @Serializable
    data object Files: VendorTab

    @Serializable
    data object Information: VendorTab
}