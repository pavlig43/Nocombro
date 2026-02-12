package ru.pavlig43.product.internal.update.tabs.composition

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.CompositionOut
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.items.product.ProductTableUi
import ru.pavlig43.mutable.api.multiLine.component.MutableTableComponent
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.mutable.api.singleLine.data.UpdateCollectionRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

internal class CompositionComponent(
    componentContext: ComponentContext,
    immutableTableDependencies: ImmutableTableDependencies,
    tabOpener: TabOpener,
    private val parentId: Int,
    repository: UpdateCollectionRepository<CompositionOut, CompositionIn>,
) : MutableTableComponent<CompositionOut, CompositionIn, CompositionUi, CompositionField>(
    componentContext = componentContext,
    parentId = parentId,
    title = "Состав",
    sortMatcher = CompositionSorter,
    filterMatcher = CompositionFilterMatcher,
    repository = repository
) {


    private val dialogNavigation = SlotNavigation<CompositionProductDialog>()

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "declaration_dialog",
        serializer = CompositionProductDialog.serializer(),
        handleBackButton = true,
    ) { dialog, context ->
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
                val compositionUi = itemList.value.first { it.composeId == dialog.composeId }
                onEvent(
                    MutableUiEvent.UpdateItem(
                        compositionUi.copy(
                            productId = product.composeId,
                            productName = product.displayName,
                            productType = product.type
                        )
                    )
                )
                dialogNavigation.dismiss()
            },
        )
    }

    override val columns: ImmutableList<ColumnSpec<CompositionUi, CompositionField, TableData<CompositionUi>>> =
        createCompositionColumn(
            onOpenProductDialog = { dialogNavigation.activate(CompositionProductDialog(it)) },
            onEvent = ::onEvent
        )


    override fun createNewItem(composeId: Int): CompositionUi {
        return CompositionUi(
            composeId = composeId,
            id = 0,
            productId = 0,
            productName = "",
            productType = null,
            count = 0
        )
    }

    override fun CompositionOut.toUi(composeId: Int): CompositionUi {
        return CompositionUi(
            composeId = composeId,
            id = id,
            productId = productId,
            productName = productName,
            productType = productType,
            count = count
        )
    }

    override fun CompositionUi.toBDIn(): CompositionIn {
        return CompositionIn(
            id = id,
            parentId = parentId,
            productId = productId,
            count = count
        )
    }


    override val errorMessages: Flow<List<String>> = itemList.map { lst: List<CompositionUi> ->
        buildList {
            lst.forEach { item ->
                if (item.productId == 0) {
                    add("В строке ${item.composeId} не указан продукт")
                }
                if (item.count == 0) {
                    add("В строке ${item.composeId} не указано количество продукта")
                }
            }
            @Suppress("MagicNumber")
            val totalSum = lst.filter {
                it.productType in arrayOf(
                    ProductType.FOOD_BASE,
                    ProductType.FOOD_PF
                )
            }.sumOf { it.count / 1000.0 }
            if (totalSum != 1.0) {
                add("Сумма в составе должна быть равна 1 кг (сейчас: ${totalSum}кг)")
            }
        }
    }


}

@Serializable
internal data class CompositionProductDialog(val composeId: Int)
