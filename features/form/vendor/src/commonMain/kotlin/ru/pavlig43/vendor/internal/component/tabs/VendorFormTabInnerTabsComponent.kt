package ru.pavlig43.vendor.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import org.koin.core.scope.Scope
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.update.component.IItemFormInnerTabsComponent
import ru.pavlig43.update.component.UpdateComponent
import ru.pavlig43.vendor.internal.component.tabs.tabslot.EssentialTabSlot
import ru.pavlig43.vendor.internal.component.tabs.tabslot.VendorFileTabSlot
import ru.pavlig43.vendor.internal.component.tabs.tabslot.VendorTabSlot
import ru.pavlig43.vendor.internal.data.VendorEssentialsUi

internal class VendorFormTabInnerTabsComponent(
    componentContext: ComponentContext,
    closeFormScreen: () -> Unit,
    componentFactory: EssentialComponentFactory<Vendor, VendorEssentialsUi>,
    scope: Scope,
    vendorId: Int
) : ComponentContext by componentContext,
    IItemFormInnerTabsComponent<VendorTab, VendorTabSlot> {

    private val dbTransaction: DataBaseTransaction = scope.get()


    override val tabNavigationComponent: TabNavigationComponent<VendorTab, VendorTabSlot> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                VendorTab.Essentials,
                VendorTab.Files,
            ),
            serializer = VendorTab.serializer(),
            slotFactory = { context, tabConfig: VendorTab,  _: () -> Unit ->
                when (tabConfig) {

                    VendorTab.Essentials -> EssentialTabSlot(
                        componentContext = context,
                        vendorId = vendorId,
                        updateRepository = scope.get(),
                        componentFactory = componentFactory
                    )


                    VendorTab.Files -> VendorFileTabSlot(
                        vendorId = vendorId,
                        dependencies = scope.get(),
                        componentContext = context
                    )

                }

            },
        )
    private suspend fun update(): Result<Unit> {
        val blocks: Value<List<suspend () -> Result<Unit>>> = tabNavigationComponent.tabChildren.map { children->
            children.items.map { child-> suspend {child.instance.onUpdate()} } }
        println(tabNavigationComponent.tabChildren.map { it.items.map { it.instance.title } })
        return dbTransaction.transaction(blocks.value)

    }
    override val updateComponent: UpdateComponent = UpdateComponent(
        componentContext = childContext("update"),
        onUpdateComponent = { update() },
        closeFormScreen = closeFormScreen,
        errorMessages = getErrors(lifecycle)
    )



}