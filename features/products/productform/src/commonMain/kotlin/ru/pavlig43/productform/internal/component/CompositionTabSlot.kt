package ru.pavlig43.productform.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.mapTo
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductComposition
import ru.pavlig43.database.data.product.ProductCompositionIn
import ru.pavlig43.database.data.product.ProductCompositionOut
import ru.pavlig43.database.data.product.ProductIngredientIn
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.itemlist.api.component.MBSItemListComponent
import ru.pavlig43.itemlist.api.data.IItemListRepository
import ru.pavlig43.loadinitdata.api.component.ILoadInitDataComponent
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.productform.internal.data.CompositionUi
import ru.pavlig43.productform.internal.data.ProductIngredientUi
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository


internal class CompositionTabSlot(
    componentContext: ComponentContext,
    private val productId: Int,
    private val productListRepository: IItemListRepository<Product, ProductType>,
    val openProductTab: (Int) -> Unit,
    private val updateCompositionRepository: UpdateCollectionRepository<ProductCompositionOut, ProductCompositionIn>
) : ComponentContext by componentContext, ProductTabSlot {
    override val title: String = "Составы"

    private val _compositionList = MutableStateFlow<List<CompositionUi>>(emptyList())
    val compositionList = _compositionList.asStateFlow()
    private val loadInitDataComponent: ILoadInitDataComponent<List<CompositionUi>> =
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

    fun removeComposition(composeKey: Int) {
        updateCompositionList { lst ->
            lst.removeIf { it.composeKey == composeKey }
            lst
        }
    }

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

    fun removeIngredients(compositionComposeKey: Int, ingredientComposeKey: Int) {
        updateIngredientsList(compositionComposeKey) { lst ->
            lst.removeIf { it.composeKey == ingredientComposeKey }
            lst
        }
    }

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

    internal val dialog: Value<ChildSlot<MBSIngredientDialog, MBSItemListComponent<Product, ProductType>>> =
        childSlot(
            source = dialogNavigation,
            key = "product_dialog",
            serializer = MBSIngredientDialog.serializer(),
            handleBackButton = true,
        ) { config: MBSIngredientDialog, context ->
            MBSItemListComponent(
                componentContext = context,
                onDismissed = dialogNavigation::dismiss,
                repository = productListRepository,
                onCreate = { openProductTab(0) },
                fullListSelection = ProductType.entries,
                onItemClick = {
                    addIngredient(config.compositionId, it.id, it.displayName)
                    dialogNavigation.dismiss()
                },
            )
        }

    private fun showDialog(compositionId: Int) {
        dialogNavigation.activate(MBSIngredientDialog(compositionId))
    }

    internal fun openDialog(compositionId: Int) {
        showDialog(compositionId)
    }


    override suspend fun onUpdate() {
        val old =
            loadInitDataComponent.firstData.value?.map { it.toProductCompositionIn(productId) }
        val new = _compositionList.value.map { it.toProductCompositionIn(productId) }
        updateCompositionRepository.update(ChangeSet(old, new))
    }


}

@Serializable
internal data class MBSIngredientDialog(val compositionId: Int)

private fun CompositionUi.toProductCompositionIn(productId: Int): ProductCompositionIn {

    val compositionForSave = ProductComposition(
        id = this.compositionId,
        productId = productId,
        name = name
    )
    val ingredients = this.productIngredients.map { it: ProductIngredientUi ->
        ProductIngredientIn(
            compositionId = compositionForSave.id,
            ingredientId = it.ingredientId,
            countGrams = it.countGram,
            id = it.id
        )
    }
    return ProductCompositionIn(
        compositionForSave = compositionForSave,
        ingredients = ingredients,
    )

}


private fun List<ProductCompositionOut>.toCompositionUi(): List<CompositionUi> {
    return this.mapIndexed { index, productCompositionOut ->
        productCompositionOut.toCompositionUi(index)
    }
}

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




