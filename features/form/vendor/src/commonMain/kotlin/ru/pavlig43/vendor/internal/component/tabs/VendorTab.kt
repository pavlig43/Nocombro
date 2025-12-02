package ru.pavlig43.vendor.internal.component.tabs

import kotlinx.serialization.Serializable


@Serializable
internal sealed interface VendorTab {
    @Serializable
    data object Essentials: VendorTab

    @Serializable
    data object Files: VendorTab


}