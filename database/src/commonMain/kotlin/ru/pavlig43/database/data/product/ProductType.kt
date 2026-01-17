package ru.pavlig43.database.data.product

import ru.pavlig43.core.model.ItemType




sealed interface ProductType: ItemType {
    val enumValue:ProductTypeEnum

    override val displayName: String get() = enumValue.displayName

    sealed interface Food : ProductType {
        data object Base : Food {
            override val enumValue: ProductTypeEnum = ProductTypeEnum.FOOD_BASE
        }

        data object Pf : Food {
            override val enumValue: ProductTypeEnum = ProductTypeEnum.FOOD_PF
        }

        data object Sale : Food {
            override val enumValue: ProductTypeEnum = ProductTypeEnum.FOOD_SALE
        }
    }

    data object Pack : ProductType {
        override val enumValue: ProductTypeEnum = ProductTypeEnum.PACK
    }


    companion object {
        val entries = listOf(
            Food.Base,
            Food.Pf,
            Food.Sale,
            Pack
        )
    }
}
enum class ProductTypeEnum: ItemType{
    FOOD_BASE { override val displayName: String = "Пищевой базовый" },
    FOOD_PF{ override val displayName: String = "Пищевой П/Ф"},
    FOOD_SALE{ override val displayName: String = "Пищевой продажа"},
    PACK{ override val displayName: String = "Упаковка"}
}



