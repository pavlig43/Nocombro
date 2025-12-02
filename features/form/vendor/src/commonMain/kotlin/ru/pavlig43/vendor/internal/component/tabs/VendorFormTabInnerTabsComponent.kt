package ru.pavlig43.vendor.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.update.component.IItemFormInnerTabsComponent
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.update.component.UpdateComponent
import ru.pavlig43.vendor.internal.component.tabs.tabslot.EssentialTabSlot
import ru.pavlig43.vendor.internal.component.tabs.tabslot.VendorFileTabSlot
import ru.pavlig43.vendor.internal.component.tabs.tabslot.VendorTabSlot
import ru.pavlig43.vendor.internal.data.VendorEssentialsUi
import ru.pavlig43.vendor.internal.di.UpdateCollectionRepositoryType

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
            slotFactory = { context, tabConfig: VendorTab, _: (VendorTab) -> Unit, _: () -> Unit ->
                when (tabConfig) {

                    VendorTab.Essentials -> EssentialTabSlot(
                        componentContext = context,
                        vendorId = vendorId,
                        updateRepository = scope.get(),
                        componentFactory = componentFactory
                    )


                    VendorTab.Files -> VendorFileTabSlot(
                        vendorId = vendorId,
                        updateRepository = scope.get(named(UpdateCollectionRepositoryType.Files.name)),
                        componentContext = context
                    )



                }

            },
        )
    private suspend fun update(): RequestResult<Unit> {
        val blocks: Value<List<suspend () -> RequestResult<Unit>>> = tabNavigationComponent.children.map { children->
            children.items.map { child-> suspend {child.instance.onUpdate()} } }
        println(tabNavigationComponent.children.map { it.items.map { it.instance.title } })
        return dbTransaction.transaction(blocks.value)

    }
    override val updateComponent: UpdateComponent = UpdateComponent(
        componentContext = childContext("update"),
        onUpdateComponent = { update() },
        closeFormScreen = closeFormScreen
    )



}