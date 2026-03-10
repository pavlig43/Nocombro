package ru.pavlig43.product.internal.update.tabs.declaration

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.DeclarationImmutableTableBuilder
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.internal.component.items.declaration.DeclarationTableUi
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.product.internal.di.ProductDeclarationRepository
import ru.pavlig43.tablecore.manger.SelectionManager
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec


internal class ProductDeclarationComponent(
    componentContext: ComponentContext,
    productId: Int,
    private val repository: ProductDeclarationRepository,
    private val tabOpener: TabOpener,
    immutableTableDependencies: ImmutableTableDependencies,
) : ComponentContext by componentContext, FormTabComponent {

    private val coroutineScope = componentCoroutineScope()
    override val title: String = "Декларации"
    override suspend fun onUpdate(): Result<Unit> {
        val old = initDataComponent.firstData.value
        val new = productDeclarations.value
        return repository.update(ChangeSet(old, new))
    }

    private val productDeclarations = MutableStateFlow<List<ProductDeclarationIn>>(emptyList())

    val initDataComponent = LoadInitDataComponent<List<ProductDeclarationIn>>(
        componentContext = childContext("init"),
        getInitData = {
            repository.getInit(productId)
        },
        onSuccessGetInitData = { lst ->
            productDeclarations.update { lst }
        }
    )
    private val dialogNavigation = SlotNavigation<DeclarationDialogConfig>()

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "declaration_dialog",
        serializer = DeclarationDialogConfig.serializer(),
        handleBackButton = true,
    ) { _, context ->
        MBSImmutableTableComponent<DeclarationTableUi>(
            componentContext = context,
            onDismissed = dialogNavigation::dismiss,
            onCreate = { tabOpener.openDeclarationTab(0) },
            dependencies = immutableTableDependencies,
            immutableTableBuilderData = DeclarationImmutableTableBuilder(
                withCheckbox = false
            ),
            onItemClick = { dec: DeclarationTableUi ->
                val declarationIn = ProductDeclarationIn(
                    productId = productId,
                    declarationId = dec.composeId,
                    id = 0
                )
                addDeclaration(declarationIn)
                dialogNavigation.dismiss()
            },
        )
    }

    private fun showDialog() {
        dialogNavigation.activate(DeclarationDialogConfig)
    }

    private val selectionManager =
        SelectionManager(
            childContext("selection")
        )
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val tableData: StateFlow<TableData<ProductDeclarationTableUi>> = combine(
        productDeclarations,
        selectionManager.selectedIdsFlow,
    ) { declarations, selectedIds ->
        declarations.map { it.declarationId } to selectedIds
    }.flatMapLatest { (declarationIds, selectedIds) ->
        repository.observeOnDeclarations(declarationIds)
            .map { declarationsList ->
                val tableUiItems = declarationsList.map { declaration ->
                    ProductDeclarationTableUi(
                        declarationId = declaration.id,
                        declarationName = declaration.displayName,
                        vendorName = declaration.vendorName,
                        isActual = declaration.isActual
                    )
                }
                TableData(
                    displayedItems = tableUiItems,
                    selectedIds = selectedIds,
                    isSelectionMode = true
                )
            }
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        TableData(isSelectionMode = true)
    )


    val columns: ImmutableList<ColumnSpec<ProductDeclarationTableUi, ProductDeclarationField, TableData<ProductDeclarationTableUi>>> =
        createProductDeclarationColumn(::onEvent)

    fun addDeclaration(declaration: ProductDeclarationIn) {
        if (declaration.declarationId in productDeclarations.value.map { it.declarationId }) return
        productDeclarations.update { it + declaration }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun onEvent(event: ProductDeclarationEvent) {
        when (event) {

            ProductDeclarationEvent.AddNew -> {
                showDialog()
            }

            is ProductDeclarationEvent.DeleteSelected -> {
                productDeclarations.update { lst->
                    val updatedList = lst - lst.filter { it.declarationId in selectionManager.selectedIds }.toSet()
                    selectionManager.clearSelected()
                    updatedList
                }
            }
            is ProductDeclarationEvent.OpenDeclaration -> {tabOpener.openDeclarationTab(event.declarationId)}
            is ProductDeclarationEvent.Selection -> {
                selectionManager.onEvent(event.selectionUiEvent)
            }
        }
    }

    override val errorMessages: Flow<List<String>> =
        tableData.map { tableData ->
            val lst = tableData.displayedItems
            buildList {
                if (lst.isEmpty()) add("Добавьте хотя бы одну декларацию")
                if (lst.none { it.isActual }) add("Хотя бы одна декларация должна быть актуальной")
                if (lst.map { it.vendorName }
                        .toSet().size > 1) add("Все декларации должны быть от одного поставщика")
            }
        }
}

@Serializable
internal data object DeclarationDialogConfig

internal sealed interface ProductDeclarationEvent {

    data class OpenDeclaration(val declarationId: Int) : ProductDeclarationEvent

    data class Selection(val selectionUiEvent: SelectionUiEvent) : ProductDeclarationEvent

    data object DeleteSelected : ProductDeclarationEvent

    data object AddNew : ProductDeclarationEvent
}
