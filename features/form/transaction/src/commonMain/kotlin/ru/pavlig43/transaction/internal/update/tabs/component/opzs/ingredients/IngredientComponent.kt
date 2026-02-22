package ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.ingredient.IngredientBD
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.BatchImmutableTableBuilder
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.items.batch.BatchTableUi
import ru.pavlig43.immutable.internal.component.items.product.ProductTableUi
import ru.pavlig43.mutable.api.multiLine.component.MutableTableComponent
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent.UpdateItem
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients.DialogChild.ImmutableMBS
import ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf.PfUi
import ua.wwind.table.ColumnSpec

internal class IngredientComponent(
    componentComponent: ComponentContext,
    private val transactionId: Int,
    private val tabOpener: TabOpener,
    private val pfFlow: StateFlow<PfUi>,
    private val immutableTableDependencies: ImmutableTableDependencies,
    repository: UpdateCollectionRepository<IngredientBD, IngredientBD>,
    private val fillIngredientsRepository: FillIngredientsRepository,
) : MutableTableComponent<IngredientBD, IngredientBD, IngredientUi, IngredientField>(
    componentContext = componentComponent,
    parentId = transactionId,
    title = "Ингредиенты",
    sortMatcher = IngredientSorter,
    filterMatcher = IngredientFilterMatcher,
    repository = repository
) {
    private val _loadCompositionState =
        MutableStateFlow<LoadCompositionState>(LoadCompositionState.Success)
    val loadCompositionState = _loadCompositionState.asStateFlow()
    private val dialogNavigation = SlotNavigation<IngredientDialog>()


    @Suppress("MagicNumber")
    fun fillFromPf() {
        val pf = pfFlow.value
        if (pf.productId == 0) return
        _loadCompositionState.update { LoadCompositionState.Loading }
        coroutineScope.launch(Dispatchers.IO) {
            loadItemsOutside(
                getData = {
                    fillIngredientsRepository.getIngredientsFromComposition(
                        productId = pf.productId,
                        transactionId = transactionId,
                        // Количество полуфабриката в кг(изначально числится в граммах)
                        countPf = pf.count.div(1000)
                    )
                },
                handleSuccess = {
                    _loadCompositionState.update {
                        LoadCompositionState.Success
                    }
                },
                handleError = { t ->
                    _loadCompositionState.update {
                        LoadCompositionState.Error(
                            t.message ?: "unknown"
                        )
                    }
                }
            )
        }


    }

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "ingredient_dialog",
        serializer = IngredientDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )

    val enabledFillButton: StateFlow<Boolean> = pfFlow.map {
        it.productId != 0 && loadCompositionState.value !is LoadCompositionState.Loading
    }.stateIn(
        coroutineScope,
        started = Eagerly,
        initialValue = false
    )

    private fun createDialogChild(
        dialogConfig: IngredientDialog,
        context: ComponentContext
    ): DialogChild {
        return when (dialogConfig) {
            is IngredientDialog.Product -> ImmutableMBS(
                MBSImmutableTableComponent<ProductTableUi>(
                    componentContext = context,
                    onDismissed = dialogNavigation::dismiss,
                    onCreate = { tabOpener.openProductTab(0) },
                    dependencies = immutableTableDependencies,
                    immutableTableBuilderData = ProductImmutableTableBuilder(
                        fullListProductTypes = ProductType.entries,
                        withCheckbox = false
                    ),
                    onItemClick = { product ->
                        val ingredientUi =
                            itemList.value.first { it.composeId == dialogConfig.composeId }
                        onEvent(
                            UpdateItem(
                                ingredientUi.copy(
                                    productId = product.composeId,
                                    productName = product.displayName,
                                    productType = product.type
                                )
                            )
                        )
                        dialogNavigation.dismiss()
                    }
                )
            )

            is IngredientDialog.Batch -> {
                ImmutableMBS(
                    MBSImmutableTableComponent<BatchTableUi>(
                        componentContext = context,
                        onDismissed = dialogNavigation::dismiss,
                        onCreate = { tabOpener.openProductTab(0) },
                        dependencies = immutableTableDependencies,
                        immutableTableBuilderData = BatchImmutableTableBuilder(
                            parentId = dialogConfig.productId
                        ),
                        onItemClick = { batch ->
                            val ingredientUi =
                                itemList.value.first { it.composeId == dialogConfig.composeId }
                            onEvent(
                                UpdateItem(
                                    ingredientUi.copy(
                                        batchId = batch.composeId,
                                        vendorName = batch.vendorName,
                                        dateBorn = batch.dateBorn
                                    )
                                )
                            )
                            dialogNavigation.dismiss()
                        }
                    )
                )
            }
        }
    }

    override val columns: ImmutableList<ColumnSpec<IngredientUi, IngredientField, TableData<IngredientUi>>> =
        createIngredientColumns(
            onOpenProductDialog = { dialogNavigation.activate(IngredientDialog.Product(it)) },
            onOpenBatchDialog = { composeId, productId ->
                dialogNavigation.activate(
                    IngredientDialog.Batch(
                        composeId, productId
                    )
                )
            },
            onEvent = ::onEvent
        )

    override fun createNewItem(composeId: Int): IngredientUi {
        return IngredientUi(
            composeId = composeId,
            transactionId = transactionId,
            id = 0,
            productType = ProductType.FOOD_BASE
        )
    }

    override fun IngredientBD.toUi(composeId: Int): IngredientUi {
        return IngredientUi(
            composeId = composeId,
            transactionId = this@IngredientComponent.transactionId,
            movementId = movementId,
            batchId = batchId,
            dateBorn = dateBorn,
            productId = productId,
            productName = productName,
            vendorName = vendorName,
            balance = count,
            productType = productType,
            id = id
        )
    }

    override fun IngredientUi.toBDIn(): IngredientBD {
        return IngredientBD(
            transactionId = transactionId,
            batchId = batchId,
            movementId = movementId,
            count = balance,
            productId = productId,
            productName = productName,
            vendorName = vendorName,
            productType = productType,
            id = id
        )
    }

    override val errorMessages: Flow<List<String>> = itemList.map { lst ->
        buildList {
            if (lst.isEmpty()) add("Не указаны ингредиенты")
            lst.forEach { ingredientUi ->
                val place = "В строке ${ingredientUi.composeId + 1}"
                if (ingredientUi.productId == 0) add("$place не указан продукт")
                if (ingredientUi.batchId == 0) add("$place не указан партия")
                if (ingredientUi.balance == 0) add("$place количество равно 0")
            }
        }
    }
}

@Serializable
internal sealed interface IngredientDialog {
    @Serializable
    data class Product(val composeId: Int) : IngredientDialog

    @Serializable
    data class Batch(val composeId: Int, val productId: Int) : IngredientDialog
}

sealed interface DialogChild {
    class ImmutableMBS(val component: MBSImmutableTableComponent<*>) : DialogChild
}

internal sealed interface LoadCompositionState {
    data object Loading : LoadCompositionState
    data object Success : LoadCompositionState
    data class Error(val message: String) : LoadCompositionState
}