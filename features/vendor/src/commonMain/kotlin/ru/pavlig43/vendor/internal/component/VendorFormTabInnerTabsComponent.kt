package ru.pavlig43.vendor.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.core.scope.get
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.tabs.DefaultTabNavigationComponent
import ru.pavlig43.core.tabs.ITabNavigationComponent
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.form.api.component.IItemFormInnerTabsComponent
import ru.pavlig43.upsertitem.api.component.IUpdateComponent
import ru.pavlig43.upsertitem.api.component.UpdateComponent
import ru.pavlig43.vendor.internal.di.UpdateCollectionRepositoryType
import ru.pavlig43.vendor.internal.di.UpdateRepositoryType

internal class VendorFormTabInnerTabsComponent(
    componentContext: ComponentContext,
    closeFormScreen: () -> Unit,
    scope: Scope,
    vendorId: Int,
    onChangeValueForMainTab: (String) -> Unit
) : ComponentContext by componentContext,
    IItemFormInnerTabsComponent<VendorTab, VendorTabSlot> {

    private val _mainTabTitle = MutableStateFlow("")
    override val mainTabTitle: StateFlow<String> = _mainTabTitle.asStateFlow()
    private val dbTransaction: DataBaseTransaction = scope.get()


    override val tabNavigationComponent: ITabNavigationComponent<VendorTab, VendorTabSlot> =
        DefaultTabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                VendorTab.RequireValues,
                VendorTab.Files,
                VendorTab.Information
            ),
            serializer = VendorTab.serializer(),
            slotFactory = { context, tabConfig: VendorTab, _: (VendorTab) -> Unit, _: () -> Unit ->
                when (tabConfig) {

                    VendorTab.RequireValues ->VendorFileTabSlot(
                        vendorId = vendorId,
                        updateRepository = scope.get(named(UpdateCollectionRepositoryType.Files.name)),
                        componentContext = context
                    )
                        //TODO
//                        VendorRequiresTabSlot(
//                        componentContext = context,
//                        vendorId = vendorId,
//                        updateRepository = scope.get(named(UpdateRepositoryType.Vendor.name)),
//                        onChangeValueForMainTab = onChangeValueForMainTab
//                    )


                    VendorTab.Files -> VendorFileTabSlot(
                        vendorId = vendorId,
                        updateRepository = scope.get(named(UpdateCollectionRepositoryType.Files.name)),
                        componentContext = context
                    )

                    VendorTab.Information -> VendorInformationTabSlot(
//                        componentContext = componentContext,
//                        vendorId = vendorId
                    )

                }

            },
        )
    private suspend fun update(): RequestResult<Unit> {
        val blocks: Value<List<suspend () -> Unit>> = tabNavigationComponent.children.map { children->
            children.items.map { child-> suspend {child.instance.onUpdate()} } }
        println(tabNavigationComponent.children.map { it.items.map { it.instance.title } })
        return dbSafeCall("document form"){
            dbTransaction.transaction(blocks.value)
        }
    }
    override val updateComponent: IUpdateComponent = UpdateComponent(
        componentContext = childContext("update"),
        onUpdateComponent = {update()},
        closeFormScreen = closeFormScreen
    )



}
