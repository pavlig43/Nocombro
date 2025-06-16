package ru.pavlig43.database.data.product

import nocombro.database.generated.resources.Res
import nocombro.database.generated.resources.base_product
import org.jetbrains.compose.resources.StringResource
import ru.pavlig43.database.data.common.data.ItemType

enum class ProductType(override val displayName: String):ItemType {
    BaseProduct("Базовый продукт")
}