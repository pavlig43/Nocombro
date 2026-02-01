package ru.pavlig43.vendor.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import org.koin.core.scope.Scope
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.update.component.IItemFormTabsComponent
import ru.pavlig43.update.component.getDefaultUpdateComponent
import ru.pavlig43.vendor.internal.component.tabs.component.VendorEssentialsComponent
import ru.pavlig43.vendor.internal.component.tabs.component.VendorFilesComponent
import ru.pavlig43.vendor.internal.data.VendorEssentialsUi

internal class VendorFormTabsComponent(
    componentContext: ComponentContext,
    closeFormScreen: () -> Unit,
    componentFactory: EssentialComponentFactory<Vendor, VendorEssentialsUi>,
    scope: Scope,
    vendorId: Int
) : ComponentContext by componentContext,
    IItemFormTabsComponent<VendorTab, VendorTabChild> {

    override val transactionExecutor: TransactionExecutor = scope.get()


    override val tabNavigationComponent: TabNavigationComponent<VendorTab, VendorTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                VendorTab.Essentials,
                VendorTab.Files,
            ),
            serializer = VendorTab.serializer(),
            tabChildFactory = { context, tabConfig: VendorTab, _: () -> Unit ->
                when (tabConfig) {

                    VendorTab.Essentials -> VendorTabChild.Essentials(
                        VendorEssentialsComponent(
                            componentContext = context,
                            vendorId = vendorId,
                            updateRepository = scope.get(),
                            componentFactory = componentFactory
                        )
                    )


                    VendorTab.Files -> VendorTabChild.Files(
                        VendorFilesComponent(
                            vendorId = vendorId,
                            dependencies = scope.get(),
                            componentContext = context
                        )
                    )

                }

            },
        )

    override val updateComponent = getDefaultUpdateComponent(componentContext,closeFormScreen)



}