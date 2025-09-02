package ru.pavlig43.productform.internal.component

import kotlinx.serialization.Serializable


@Serializable
internal sealed interface ProductTab {
    @Serializable
    data object RequireValues: ProductTab
    @Serializable
    data object Files: ProductTab

    @Serializable
    data object Declaration: ProductTab
}