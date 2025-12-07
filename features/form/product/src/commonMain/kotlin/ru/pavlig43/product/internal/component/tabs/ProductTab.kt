package ru.pavlig43.product.internal.component.tabs

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface ProductTab {
    @Serializable
    data object Essentials: ProductTab
    @Serializable
    data object Files: ProductTab

    @Serializable
    data object Declaration: ProductTab

    @Serializable
    data object Ingredients: ProductTab
}