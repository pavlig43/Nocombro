package ru.pavlig43.declarationform.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import org.koin.core.scope.Scope
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.mapTo
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.declarationform.internal.data.RequiresValuesWithDate
import ru.pavlig43.declarationform.internal.toDeclarationWithDate
import ru.pavlig43.itemlist.api.component.VendorListParamProvider
import ru.pavlig43.itemlist.api.component.refactoring.ItemListDependencies
import ru.pavlig43.itemlist.api.component.refactoring.MBSItemListComponent1
import ru.pavlig43.loadinitdata.api.component.ILoadInitDataComponent
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent


internal class DeclarationRequiresComponent(
    componentContext: ComponentContext,
    itemListDependencies: ItemListDependencies,
    private val onChangeValueForMainTab: (String) -> Unit,
    private val openVendorTab: (Int) -> Unit,
    private val getInitData: (suspend () -> RequestResult<DeclarationIn>)?,
) : ComponentContext by componentContext {


    private val coroutineScope = componentCoroutineScope()

    private val dialogNavigation = SlotNavigation<MBSVendorDialog>()

    internal val dialog: Value<ChildSlot<MBSVendorDialog, MBSItemListComponent1>> =
        childSlot(
            source = dialogNavigation,
            key = "vendor_dialog",
            serializer = MBSVendorDialog.serializer(),
            handleBackButton = true,
        ) { config: MBSVendorDialog, context ->
            MBSItemListComponent1(
                componentContext = context,
                onDismissed = dialogNavigation::dismiss,
                itemListDependencies = itemListDependencies,
                onCreate = { openVendorTab(0) },
                itemListParamProvider = VendorListParamProvider(
                    withCheckbox = false
                ),
                onItemClick = {
                    changeVendor(it.id, it.displayName)
                    dialogNavigation.dismiss()
                },
            )
        }


    fun showDialog() {
        dialogNavigation.activate(MBSVendorDialog)
    }


    private val _requiresValuesWithDate: MutableStateFlow<RequiresValuesWithDate> =
        MutableStateFlow(RequiresValuesWithDate())

    internal val requiresValuesWithDate: StateFlow<RequiresValuesWithDate> =
        _requiresValuesWithDate.asStateFlow()


    init {
        _requiresValuesWithDate.onEach {
            val tabTitle = "${it.vendorName ?: ""} ${it.name}"
            onChangeValueForMainTab(tabTitle)
        }.launchIn(coroutineScope)
    }

    fun changeBestBefore(bestBefore: Long?) {
        _requiresValuesWithDate.update { it.copy(bestBefore = bestBefore) }
    }
    fun onChangeCheckedNotificationVisible(isObserveFromNotification: Boolean){
        _requiresValuesWithDate.update { it.copy(isObserveFromNotification = isObserveFromNotification) }
    }


    val initComponent: ILoadInitDataComponent<RequiresValuesWithDate> = LoadInitDataComponent(
        componentContext = childContext("initComponent"),
        getInitData = {
            getInitData?.invoke()?.mapTo { it.toDeclarationWithDate() } ?: RequestResult.Success(
                RequiresValuesWithDate()
            )
        },
        onSuccessGetInitData = { requireValues ->
            _requiresValuesWithDate.update { requireValues }
        }
    )

    fun onNameChange(name: String) {
        _requiresValuesWithDate.update { it.copy(name = name) }
    }


    val isValidAllValue: Flow<Boolean> = _requiresValuesWithDate.map {
        println(it)
        it.name.isNotBlank() &&
        it.vendorId != null &&
        it.bestBefore != null
    }


    private fun changeVendor(vendorId: Int, vendorName: String) {
        _requiresValuesWithDate.update { it.copy(vendorId = vendorId, vendorName = vendorName) }
    }


}

@Serializable
internal data object MBSVendorDialog


