package ru.pavlig43.declaration.api.component

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
import ru.pavlig43.core.FormTabSlot
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.GenericDeclarationIn
import ru.pavlig43.core.data.GenericDeclarationOut
import ru.pavlig43.core.mapTo
import ru.pavlig43.declaration.api.data.ProductDeclarationUi
import ru.pavlig43.declaration.internal.component.MBSDeclarationListComponent
import ru.pavlig43.declarationlist.internal.data.DeclarationItemUi
import ru.pavlig43.declarationlist.internal.data.DeclarationListRepository
import ru.pavlig43.loadinitdata.api.component.ILoadInitDataComponent
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

abstract class DeclarationTabSlot<Out : GenericDeclarationOut, In : GenericDeclarationIn>(
    componentContext: ComponentContext,
    private val productId: Int,
    private val declarationListRepository: DeclarationListRepository,
    private val updateRepository: UpdateCollectionRepository<Out, In>,
    openDeclarationTab: (Int) -> Unit,
    private val mapper: ProductDeclarationUi.(id: Int) -> In,
) : ComponentContext by componentContext, FormTabSlot {
    override val title: String = "Декларации"


    internal val productDeclarationList = ProductDeclarationListComponent<Out>(
        componentContext = childContext("declarationList"),
        getInitData = { updateRepository.getInit(productId) },
        openDeclarationTab = openDeclarationTab
    )
    private val dialogNavigation = SlotNavigation<DialogConfig>()

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "document_dialog",
        serializer = DialogConfig.serializer(),
        handleBackButton = true,
    ) { config, context ->
        MBSDeclarationListComponent(
            componentContext = context,
            onDismissed = dialogNavigation::dismiss,
            onCreate = { openDeclarationTab(0) },
            repository = declarationListRepository,
            onItemClick = {dec: DeclarationItemUi ->
                productDeclarationList.addDeclaration(dec)
                dialogNavigation.dismiss()
            },
        )
    }

    private fun showDialog() {
        dialogNavigation.activate(DialogConfig)
    }

    internal fun openDialog() {
        showDialog()
    }


    override suspend fun onUpdate() {
        val old =
            productDeclarationList.loadInitDataComponent.firstData.value?.map { it.mapper(productId) }
        val new = productDeclarationList.declarationUi.value.map { it.mapper(productId) }
        updateRepository.update(ChangeSet(old, new))
    }

}


internal class ProductDeclarationListComponent<Out : GenericDeclarationOut>(
    componentContext: ComponentContext,
    val openDeclarationTab: (Int) -> Unit,
    private val getInitData: suspend () -> RequestResult<List<Out>>,
) : ComponentContext by componentContext {

    fun removeDeclaration(index: Int) {
        updateList { lst: MutableList<ProductDeclarationUi> ->
            lst.removeIf { it.composeKey == index }
            lst
        }
    }


    fun addDeclaration(declaration: DeclarationItemUi) {
        if (declaration.id in _productDeclarationUiList.value.map { it.declarationId }) return
        val composeKey = _productDeclarationUiList.value.maxOfOrNull { it.composeKey }?.plus(1) ?: 0
        val productDeclarationUi = ProductDeclarationUi(
            id = 0,
            declarationId = declaration.id,
            composeKey = composeKey,
            declarationName = declaration.displayName,
            vendorName = declaration.vendorName,
            isActual = true,
            bestBefore = declaration.bestBefore
        )

        updateList { lst ->
            lst.add(productDeclarationUi)
            lst
        }
    }


    private fun updateList(updateAction: (MutableList<ProductDeclarationUi>) -> List<ProductDeclarationUi>) {
        val updatedDeclarations = _productDeclarationUiList.value.toMutableList()
        _productDeclarationUiList.update { updateAction(updatedDeclarations) }
    }


    private val _productDeclarationUiList = MutableStateFlow<List<ProductDeclarationUi>>(emptyList())
    val declarationUi = _productDeclarationUiList.asStateFlow()
    val loadInitDataComponent: ILoadInitDataComponent<List<ProductDeclarationUi>> =
        LoadInitDataComponent<List<ProductDeclarationUi>>(
            componentContext = childContext("loadInitData"),
            getInitData = { getInitData().mapTo { it.toListDeclarationUi() } },
            onSuccessGetInitData = { declarations -> _productDeclarationUiList.update { declarations } }
        )


}

@Serializable
internal data object DialogConfig

private fun <D : GenericDeclarationOut> List<D>.toListDeclarationUi(): List<ProductDeclarationUi> {
    return this.mapIndexed { ind, declaration -> declaration.toDeclarationUi(ind) }
}


@OptIn(ExperimentalTime::class)
private fun <D : GenericDeclarationOut> D.toDeclarationUi(composeKey: Int): ProductDeclarationUi {
    return ProductDeclarationUi(
        id = id,
        declarationId = declarationId,
        isActual = bestBefore > Clock.System.now().toEpochMilliseconds(),
        composeKey = composeKey,
        declarationName = declarationName,
        vendorName = vendorName,
        bestBefore = bestBefore
    )
}


