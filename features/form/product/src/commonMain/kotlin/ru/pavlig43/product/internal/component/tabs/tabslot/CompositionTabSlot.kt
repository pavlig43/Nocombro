package ru.pavlig43.product.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.mapTo
import ru.pavlig43.database.data.product.ProductComposition
import ru.pavlig43.database.data.product.ProductCompositionIn
import ru.pavlig43.database.data.product.ProductCompositionOut
import ru.pavlig43.database.data.product.ProductIngredientIn
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.itemlist.statik.ItemStaticListDependencies
import ru.pavlig43.itemlist.core.refac.api.ProductListParamProvider
import ru.pavlig43.itemlist.statik.api.component.MBSItemListComponent
import ru.pavlig43.itemlist.statik.internal.component.ProductItemUi
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.product.internal.data.CompositionUi
import ru.pavlig43.product.internal.data.ProductIngredientUi
import ru.pavlig43.update.data.UpdateCollectionRepository


/**
 * Компонент вкладки "Составы" в форме продукта.
 *
 * Отвечает за управление составами продукта, включая добавление, удаление и редактирование
 * составов и их ингредиентов. Также поддерживает загрузку начальных данных и сохранение изменений.
 *
 * @param componentContext Контекст компонента Decompose.
 * @param productId Идентификатор продукта, к которому относятся составы.
 * @param openProductTab Функция открытия вкладки продукта по ID.
 * @param updateCompositionRepository Репозиторий для обновления составов продукта.
 */
@Suppress("TooManyFunctions")
internal class CompositionTabSlot(
    componentContext: ComponentContext,
    private val productId: Int,
    itemStaticListDependencies: ItemStaticListDependencies,
    val openProductTab: (Int) -> Unit,
    private val updateCompositionRepository: UpdateCollectionRepository<ProductCompositionOut, ProductCompositionIn>
) : ComponentContext by componentContext, ProductTabSlot {
    override val title: String = "Составы"

    private val _compositionList = MutableStateFlow<List<CompositionUi>>(emptyList())

    /**
     * Состояние списка составов продукта.
     *
     * @see CompositionUi
     */
    val compositionList = _compositionList.asStateFlow()
    private val loadInitDataComponent: LoadInitDataComponent<List<CompositionUi>> =
        LoadInitDataComponent<List<CompositionUi>>(
            componentContext = childContext("loadInitData_composition"),
            getInitData = {
                updateCompositionRepository.getInit(productId).mapTo { it.toCompositionUi() }
            },
            onSuccessGetInitData = { compositions ->
                _compositionList.update { compositions }
            }
        )

    private fun updateCompositionList(updateAction: (MutableList<CompositionUi>) -> List<CompositionUi>) {
        val productCompositions = _compositionList.value.toMutableList()
        _compositionList.update { updateAction(productCompositions) }
    }

    /**
     * Удаляет состав по его ключу.
     *
     * @param composeKey Уникальный ключ состава для удаления.
     */
    fun removeComposition(composeKey: Int) {
        updateCompositionList { lst ->
            lst.removeIf { it.composeKey == composeKey }
            lst
        }
    }

    /**
     * Добавляет новый пустой состав в список.
     *
     * Новый состав получает уникальный composeKey, основанный на максимальном значении в списке.
     */
    fun addNewComposition() {
        val composeKey = _compositionList.value.maxOfOrNull { it.composeKey }?.plus(1) ?: 0
        val composition = CompositionUi(
            compositionId = 0,
            composeKey = composeKey,
        )

        updateCompositionList { lst ->
            lst.add(composition)
            lst
        }
    }

    /**
     * Обновляет имя состава по его ключу.
     *
     * @param composeKey Уникальный ключ состава.
     * @param name Новое имя состава.
     */
    fun onChangeName(composeKey: Int, name: String) {
        updateCompositionList { lst ->
            lst.firstOrNull { it.composeKey == composeKey }
                ?: return@updateCompositionList lst
            val updatedComposition = lst.removeAt(composeKey).copy(name = name)
            lst.add(composeKey, updatedComposition)
            lst
        }

    }

    ////////
    private fun updateIngredientsList(
        compositionComposeKey: Int,
        updateAction: (MutableList<ProductIngredientUi>) -> List<ProductIngredientUi>
    ) {
        val composition =
            _compositionList.value.firstOrNull { it.composeKey == compositionComposeKey } ?: return
        val productIngredients = composition.productIngredients.toMutableList()
        val updatedIngredients = updateAction(productIngredients)
        val updatedComposition = composition.copy(productIngredients = updatedIngredients)
        updateCompositionList { lst ->
            lst.removeIf { it.composeKey == compositionComposeKey }
            lst.add(compositionComposeKey, updatedComposition)
            lst

        }
    }

    /**
     * Удаляет ингредиент из состава по ключам состава и ингредиента.
     *
     * @param compositionComposeKey Ключ состава.
     * @param ingredientComposeKey Ключ ингредиента в составе.
     */
    fun removeIngredients(compositionComposeKey: Int, ingredientComposeKey: Int) {
        updateIngredientsList(compositionComposeKey) { lst ->
            lst.removeIf { it.composeKey == ingredientComposeKey }
            lst
        }
    }
@Suppress("ReturnCount")
    private fun addIngredient(compositionComposeKey: Int, productId: Int, productName: String) {
        val composition =
            _compositionList.value.firstOrNull { it.composeKey == compositionComposeKey } ?: return
        if (productId in composition.productIngredients.map { it.ingredientId }) return
        if (productId == this.productId) return
        val composeKey = composition.productIngredients.maxOfOrNull { it.composeKey }?.plus(1) ?: 0

        val ingredientUi = ProductIngredientUi(
            id = 0,
            composeKey = composeKey,
            ingredientId = productId,
            countGram = 0,
            name = productName
        )
        updateIngredientsList(compositionComposeKey) { lst ->
            lst.add(ingredientUi)
            lst
        }
    }

    /**
     * Изменяет количество граммов ингредиента в составе.
     *
     * @param compositionComposeKey Ключ состава.
     * @param ingredientComposeKey Ключ ингредиента.
     * @param gram Новое значение в граммах.
     */
    fun onChangeGram(compositionComposeKey: Int, ingredientComposeKey: Int, gram: Int) {
        val composition =
            _compositionList.value.firstOrNull { it.composeKey == compositionComposeKey } ?: return
        val updatedIngredients = composition.productIngredients.toMutableList()
        val ingredient = updatedIngredients.removeAt(ingredientComposeKey).copy(countGram = gram)
        updatedIngredients.add(ingredientComposeKey, ingredient)
        val updatedComposition = composition.copy(productIngredients = updatedIngredients)
        updateCompositionList { lst ->
            lst.removeIf { it.composeKey == compositionComposeKey }
            lst.add(compositionComposeKey, updatedComposition)
            lst
        }
    }

    private val dialogNavigation = SlotNavigation<MBSIngredientDialog>()

    internal val dialog =
        childSlot(
            source = dialogNavigation,
            key = "product_dialog",
            serializer = MBSIngredientDialog.serializer(),
            handleBackButton = true,
        ) { config: MBSIngredientDialog, context ->
            MBSItemListComponent<ProductItemUi>(
                componentContext = context,
                onDismissed = dialogNavigation::dismiss,
                itemStaticListDependencies = itemStaticListDependencies,
                onCreate = { openProductTab(0) },
                immutableTableBuilder = ProductListParamProvider(
                    fullListProductTypes = ProductType.entries,
                    withCheckbox = false
                ),
                onItemClick = {
                    addIngredient(config.compositionId, it.id, it.displayName)
                    dialogNavigation.dismiss()
                },
            )
        }

    private fun showDialog(compositionId: Int) {
        dialogNavigation.activate(MBSIngredientDialog(compositionId))
    }

    /**
     * Открывает диалог выбора ингредиента для добавления в состав.
     *
     * @param compositionId Ключ состава, в который будет добавлен ингредиент.
     */
    internal fun openDialog(compositionId: Int) {
        showDialog(compositionId)
    }


    /**
     * Выполняет сохранение изменений в составах продукта.
     *
     * Сравнивает текущее состояние с исходным и отправляет изменения через репозиторий.
     */
    override suspend fun onUpdate(): RequestResult<Unit> {
        val old =
            loadInitDataComponent.firstData.value?.map { it.toProductCompositionIn(productId) }
        val new = _compositionList.value.map { it.toProductCompositionIn(productId) }
        return updateCompositionRepository.update(ChangeSet(old, new))
    }


}

/**
 * Конфигурация диалога выбора ингредиента.
 *
 * @property compositionId Ключ состава, в который будет добавлен ингредиент.
 */
@Serializable
internal data class MBSIngredientDialog(val compositionId: Int)

/**
 * Преобразует [CompositionUi] в [ProductCompositionIn] для сохранения в репозитории.
 *
 * @param productId Идентификатор родительского продукта.
 * @return Объект [ProductCompositionIn].
 */
private fun CompositionUi.toProductCompositionIn(productId: Int): ProductCompositionIn {

    val compositionForSave = ProductComposition(
        id = this.compositionId,
        productId = productId,
        name = name
    )
    val ingredients = this.productIngredients.map { ingredient: ProductIngredientUi ->
        ProductIngredientIn(
            compositionId = compositionForSave.id,
            ingredientId = ingredient.ingredientId,
            countGrams = ingredient.countGram,
            id = ingredient.id
        )
    }
    return ProductCompositionIn(
        compositionForSave = compositionForSave,
        ingredients = ingredients,
    )

}


/**
 * Преобразует список [ProductCompositionOut] в список [CompositionUi].
 *
 * Каждый элемент получает уникальный composeKey на основе индекса.
 *
 * @return Список [CompositionUi].
 */
private fun List<ProductCompositionOut>.toCompositionUi(): List<CompositionUi> {
    return this.mapIndexed { index, productCompositionOut ->
        productCompositionOut.toCompositionUi(index)
    }
}

/**
 * Преобразует [ProductCompositionOut] в [CompositionUi] с заданным ключом.
 *
 * @param composeKey Уникальный ключ состава в UI.
 * @return Объект [CompositionUi].
 */
private fun ProductCompositionOut.toCompositionUi(composeKey: Int): CompositionUi {
    val ingredientsUi = ingredients.mapIndexed { index, productIngredientOut ->
        ProductIngredientUi(
            id = productIngredientOut.ingredient.id,
            ingredientId = productIngredientOut.ingredient.ingredientId,
            composeKey = index,
            countGram = productIngredientOut.ingredient.countGrams,
            name = productIngredientOut.product.displayName
        )
    }
    return CompositionUi(
        compositionId = composition.id,
        composeKey = composeKey,
        name = composition.name,
        productIngredients = ingredientsUi
    )
}
