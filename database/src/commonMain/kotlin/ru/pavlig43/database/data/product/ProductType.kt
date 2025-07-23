package ru.pavlig43.database.data.product

import ru.pavlig43.database.data.common.data.ItemType

enum class ProductType(override val displayName: String):ItemType {
    BaseProduct("Базовый продукт")
}