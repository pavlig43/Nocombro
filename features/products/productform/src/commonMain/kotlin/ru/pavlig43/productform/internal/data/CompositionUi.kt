package ru.pavlig43.productform.internal.data

/**
 * Представление UI модели состава продукта.
 *
 * @property compositionId Идентификатор состава для бд.
 * @property composeKey Ключ для Compose, используемый для отслеживания изменений.
 * @property name Название состава.
 * @property productIngredients Список ингредиентов, входящих в состав.
 */
data class CompositionUi(
    val compositionId: Int,
    val composeKey: Int,
    val name: String = "",
    val productIngredients: List<ProductIngredientUi> = emptyList()
)
/**
 * Представление UI модели ингредиента продукта.
 *
 * @property id Идентификатор строки в составе Composition в бд( ).
 * @property composeKey Ключ для Compose, используемый для отслеживания изменений.
 * @property ingredientId Идентификатор самого ингредиента, который продукт(например, соль с id = 3).
 * @property countGram Количество ингредиента в граммах.
 * @property name Название ингредиента.
 */
data class ProductIngredientUi(
    val id: Int,
    val composeKey: Int,
    val ingredientId: Int,
    val countGram: Int,
    val name: String,
)