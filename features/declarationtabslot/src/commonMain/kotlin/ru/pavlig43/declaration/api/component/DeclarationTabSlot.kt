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
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.GenericDeclarationIn
import ru.pavlig43.core.data.GenericDeclarationOut
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.declaration.api.data.ItemDeclarationUi
import ru.pavlig43.itemlist.api.component.DeclarationBuilder
import ru.pavlig43.itemlist.api.component.MBSImmutableTableComponent
import ru.pavlig43.itemlist.api.dependencies
import ru.pavlig43.itemlist.internal.component.items.declaration.DeclarationTableUi
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.update.data.UpdateCollectionRepository
import kotlin.time.ExperimentalTime

abstract class DeclarationTabSlot<Out : GenericDeclarationOut, In : GenericDeclarationIn>(
    componentContext: ComponentContext,
    private val productId: Int,
    dependencies: dependencies,
    private val updateRepository: UpdateCollectionRepository<Out, In>,
    openDeclarationTab: (Int) -> Unit,
    private val mapper: ItemDeclarationUi.(id: Int) -> In,
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
    ) { _, context ->
        MBSImmutableTableComponent<DeclarationTableUi>(
            componentContext = context,
            onDismissed = dialogNavigation::dismiss,
            onCreate = { openDeclarationTab(0) },
            dependencies = dependencies,
            builderData = DeclarationBuilder(
                withCheckbox = false
            ),
            onItemClick = {dec ->
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


    override suspend fun onUpdate(): Result<Unit> {
        val old =
            productDeclarationList.loadInitDataComponent.firstData.value?.map { it.mapper(productId) }
        val new = productDeclarationList.declarationUi.value.map { it.mapper(productId) }
        return updateRepository.update(ChangeSet(old, new))
    }

}


internal class ProductDeclarationListComponent<Out : GenericDeclarationOut>(
    componentContext: ComponentContext,
    val openDeclarationTab: (Int) -> Unit,
    private val getInitData: suspend () -> Result<List<Out>>,
) : ComponentContext by componentContext {

    fun removeDeclaration(index: Int) {
        updateList { lst: MutableList<ItemDeclarationUi> ->
            lst.removeIf { it.composeKey == index }
            lst
        }
    }


    fun addDeclaration(declaration: DeclarationTableUi) {
        if (declaration.composeId in _itemDeclarationUiList.value.map { it.declarationId }) return
        val composeKey = _itemDeclarationUiList.value.maxOfOrNull { it.composeKey }?.plus(1) ?: 0
        val itemDeclarationUi = ItemDeclarationUi(
            id = 0,
            declarationId = declaration.composeId,
            composeKey = composeKey,
            declarationName = declaration.displayName,
            vendorName = declaration.vendorName,
            isActual = true,

            bestBefore = getCurrentLocalDate()
        )

        updateList { lst ->
            lst.add(itemDeclarationUi)
            lst
        }
    }


    private fun updateList(updateAction: (MutableList<ItemDeclarationUi>) -> List<ItemDeclarationUi>) {
        val updatedDeclarations = _itemDeclarationUiList.value.toMutableList()
        _itemDeclarationUiList.update { updateAction(updatedDeclarations) }
    }


    private val _itemDeclarationUiList = MutableStateFlow<List<ItemDeclarationUi>>(emptyList())
    val declarationUi = _itemDeclarationUiList.asStateFlow()
    val loadInitDataComponent: LoadInitDataComponent<List<ItemDeclarationUi>> =
        LoadInitDataComponent<List<ItemDeclarationUi>>(
            componentContext = childContext("loadInitData"),
            getInitData = { getInitData().map { it.toListDeclarationUi() } },
            onSuccessGetInitData = { declarations -> _itemDeclarationUiList.update { declarations } }
        )


}

@Serializable
internal data object DialogConfig

private fun <D : GenericDeclarationOut> List<D>.toListDeclarationUi(): List<ItemDeclarationUi> {
    return this.mapIndexed { ind, declaration -> declaration.toDeclarationUi(ind) }
}


@OptIn(ExperimentalTime::class)
private fun <D : GenericDeclarationOut> D.toDeclarationUi(composeKey: Int): ItemDeclarationUi {
    return ItemDeclarationUi(
        id = id,
        declarationId = declarationId,
        isActual = bestBefore > getCurrentLocalDate(),
        composeKey = composeKey,
        declarationName = declarationName,
        vendorName = vendorName,
        bestBefore = bestBefore
    )
}


