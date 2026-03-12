package ru.pavlig43.vendor.internal.update

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import org.koin.core.scope.Scope
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.update.component.IItemFormTabsComponent
import ru.pavlig43.update.component.getDefaultUpdateComponent
import ru.pavlig43.vendor.internal.model.VendorEssentialsUi
import ru.pavlig43.vendor.internal.update.tabs.VendorFilesComponent
import ru.pavlig43.vendor.internal.update.tabs.essential.VendorUpdateSingleLineComponent

internal class VendorFormTabsComponent(
    componentContext: ComponentContext,
    componentFactory: SingleLineComponentFactory<Vendor, VendorEssentialsUi>,
    scope: Scope,
    vendorId: Int,
    observeOnVendor: (VendorEssentialsUi) -> Unit,
) : ComponentContext by componentContext,
    IItemFormTabsComponent<VendorTab, VendorTabChild> {

    override val transactionExecutor: TransactionExecutor = scope.get()

    override val tabNavigationComponent: TabNavigationComponent<VendorTab, VendorTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                VendorTab.Essential,
                VendorTab.Files
            ),
            serializer = VendorTab.serializer(),
            tabChildFactory = { context, tabConfig: VendorTab, _: () -> Unit ->
                when (tabConfig) {

                    VendorTab.Files -> VendorTabChild.Files(
                        VendorFilesComponent(
                            vendorId = vendorId,
                            dependencies = scope.get(),
                            componentContext = context
                        )
                    )

                    VendorTab.Essential -> VendorTabChild.Essential(
                        VendorUpdateSingleLineComponent(
                            componentContext = context,
                            vendorId = vendorId,
                            updateRepository = scope.get(),
                            observeOnItem = observeOnVendor,
                            onSuccessInitData = observeOnVendor,
                            componentFactory = componentFactory
                        )
                    )
                }
            },
        )

    override val updateComponent = getDefaultUpdateComponent(componentContext)
}
