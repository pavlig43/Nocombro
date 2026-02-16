package ru.pavlig43.product.internal.update.tabs.declaration

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
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductDeclarationOut
import ru.pavlig43.flowImmutable.api.component.FlowMultilineComponent
import ru.pavlig43.flowImmutable.api.data.FlowMultilineRepository
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.DeclarationImmutableTableBuilder
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.internal.component.items.declaration.DeclarationTableUi
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import kotlin.time.ExperimentalTime

internal class ProductDeclarationComponent(
    componentContext: ComponentContext,
    productId: Int,
    observableRepository: FlowMultilineRepository<ProductDeclarationOut, ProductDeclarationIn>,
    tabOpener: TabOpener,
    immutableTableDependencies: ImmutableTableDependencies,
): FlowMultilineComponent<ProductDeclarationOut, ProductDeclarationIn, FlowProductDeclarationTableUi, ProductDeclarationField>(
    componentContext = componentContext,
    parentId = productId,
    getObservableId = { it.declarationId },
    mapper = { toUi(it) },
    repository = observableRepository,
    filterMatcher = ProductDeclarationFilterMatcher,
    sortMatcher = ProductDeclarationSorter,
    onRowClick = { tabOpener.openDeclarationTab(it.declarationId) },
) {
    override val title: String = "Декларации"
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
                addParentBD(declarationIn)
                dialogNavigation.dismiss()
            },
        )
    }

    private fun showDialog() {
        dialogNavigation.activate(DeclarationDialogConfig)
    }

    override val columns: ImmutableList<ColumnSpec<FlowProductDeclarationTableUi, ProductDeclarationField, TableData<FlowProductDeclarationTableUi>>> =
        createProductDeclarationColumn(::onEvent, ::showDialog)

    override val errorMessages: Flow<List<String>> = uiList.map { lst: List<FlowProductDeclarationTableUi> ->
        buildList {
            if (lst.isEmpty()) add("Добавьте хотя бы одну декларацию")
            if (lst.none { it.isActual }) add("Хотя бы одна декларация должна быть актуальной")
        }
    }
}

@Serializable
internal data object DeclarationDialogConfig

@OptIn(ExperimentalTime::class)
private fun ProductDeclarationOut.toUi(composeKey: Int): FlowProductDeclarationTableUi {
    return FlowProductDeclarationTableUi(
        id = id,
        declarationId = declarationId,
        isActual = isActual,
        composeId = composeKey,
        declarationName = declarationName,
        vendorName = vendorName
    )
}
