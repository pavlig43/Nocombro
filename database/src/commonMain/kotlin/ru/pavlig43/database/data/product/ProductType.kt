package ru.pavlig43.database.data.product

import ru.pavlig43.core.model.ItemType





enum class ProductType: ItemType{
    FOOD_BASE { override val displayName: String = "Пищевой базовый" },
    FOOD_PF{ override val displayName: String = "Пищевой П/Ф"},
    PACK{ override val displayName: String = "Упаковка"}
}



