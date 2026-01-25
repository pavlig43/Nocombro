package ru.pavlig43.product.internal.component.tabs.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductDeclarationOut
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.DeclarationImmutableTableBuilder
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.internal.component.items.declaration.DeclarationTableUi
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.update.data.UpdateCollectionRepository
import kotlin.time.ExperimentalTime


class ProductDeclarationComponent(
    componentContext: ComponentContext,
    private val productId: Int,
    dependencies: ImmutableTableDependencies,
    private val updateRepository: UpdateCollectionRepository<ProductDeclarationOut, ProductDeclarationIn>,
    tabOpener: TabOpener,
) : ComponentContext by componentContext, FormTabComponent {
    override val title: String = "Декларации"


    internal val declarationListComponent = DeclarationListComponent(
        componentContext = childContext("declarationList"),
        getInitData = { updateRepository.getInit(productId) },
        tabOpener = tabOpener
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
            dependencies = dependencies,
            immutableTableBuilderData = DeclarationImmutableTableBuilder(
                withCheckbox = false
            ),
            onItemClick = { dec ->
                declarationListComponent.addDeclaration(dec)
                dialogNavigation.dismiss()
            },
        )
    }

    private fun showDialog() {
        dialogNavigation.activate(DeclarationDialogConfig)
    }

    internal fun openDialog() {
        showDialog()
    }


    override suspend fun onUpdate(): Result<Unit> {
        val old =
            declarationListComponent.loadInitDataComponent.firstData.value?.map {
                it.mapper(
                    productId
                )
            }
        val new = declarationListComponent.declarationList.value.map { it.mapper(productId) }
        return updateRepository.update(ChangeSet(old, new))
    }

    override val errorMessages: Flow<List<String>> =
        declarationListComponent.declarationList.map { lst ->
            buildList {
                if (lst.isEmpty()) {
                    add("Необходимо добавить хотя бы одну декларацию")
                }
                if (lst.all { !it.isActual }) {
                    add("Все декларации просрочены")
                }
            }
        }


}


internal class DeclarationListComponent(
    componentContext: ComponentContext,
    private val tabOpener: TabOpener,
    private val getInitData: suspend () -> Result<List<ProductDeclarationOut>>,
) : ComponentContext by componentContext {

    fun removeDeclaration(index: Int) {
        _declarationList.update { lst ->
            lst.toMutableList().apply { removeAll { it.composeKey == index } }
        }
    }
    fun openDeclarationTab(id: Int){
        tabOpener.openDeclarationTab(id)
    }


    fun addDeclaration(declaration: DeclarationTableUi) {
        if (declaration.composeId in _declarationList.value.map { it.declarationId }) return
        val composeKey = _declarationList.value.maxOfOrNull { it.composeKey }?.plus(1) ?: 0
        val itemDeclarationUi = DeclarationUi(
            id = 0,
            declarationId = declaration.composeId,
            composeKey = composeKey,
            declarationName = declaration.displayName,
            vendorName = declaration.vendorName,
            isActual = declaration.isActual,
        )
        _declarationList.update {
            it + itemDeclarationUi

        }


    }


    private val _declarationList = MutableStateFlow<List<DeclarationUi>>(emptyList())
    val declarationList = _declarationList.asStateFlow()
    val loadInitDataComponent: LoadInitDataComponent<List<DeclarationUi>> =
        LoadInitDataComponent<List<DeclarationUi>>(
            componentContext = childContext("loadInitData"),
            getInitData = { getInitData().map { it.toListDeclarationUi() } },
            onSuccessGetInitData = { declarations -> _declarationList.update { declarations } }
        )


}

@Serializable
internal data object DeclarationDialogConfig

private fun List<ProductDeclarationOut>.toListDeclarationUi(): List<DeclarationUi> {
    return this.mapIndexed { ind, declaration -> declaration.toUi(ind) }
}


@OptIn(ExperimentalTime::class)
private fun ProductDeclarationOut.toUi(composeKey: Int): DeclarationUi {
    return DeclarationUi(
        id = id,
        declarationId = declarationId,
        isActual = isActual,
        composeKey = composeKey,
        declarationName = declarationName,
        vendorName = vendorName,
    )
}

private fun DeclarationUi.mapper(productId: Int): ProductDeclarationIn {
    return ProductDeclarationIn(
        productId = productId,
        declarationId = declarationId,
        id = id
    )
}

data class DeclarationUi(
    val id: Int,
    val composeKey: Int,
    val declarationId: Int,
    val declarationName: String,
    val vendorName: String,
    val isActual: Boolean,
)





