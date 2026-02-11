package ru.pavlig43.vendor.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import org.koin.core.scope.Scope
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.vendor.api.VendorFormDependencies
import ru.pavlig43.vendor.internal.create.component.CreateVendorSingleLineComponent
import ru.pavlig43.vendor.internal.model.VendorEssentialsUi
import ru.pavlig43.vendor.internal.model.toUi
import ru.pavlig43.vendor.internal.di.createVendorFormModule
import ru.pavlig43.vendor.internal.update.VendorFormTabsComponent

class VendorFormComponent(
    vendorId: Int,
    val closeTab: () -> Unit,
    componentContext: ComponentContext,
    dependencies: VendorFormDependencies,
) : ComponentContext by componentContext, MainTabComponent {

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createVendorFormModule(dependencies))

    private val _model = MutableStateFlow(MainTabComponent.NavTabState(""))
    override val model = _model.asStateFlow()

    private val stackNavigation = StackNavigation<Config>()

    private val essentialsComponentFactory = SingleLineComponentFactory<Vendor, VendorEssentialsUi>(
        initItem = VendorEssentialsUi(),
        errorFactory = { item: VendorEssentialsUi ->
            buildList {
                if (item.displayName.isBlank()) add("Название поставщика обязательно")
            }
        },
        mapperToUi = { toUi() }
    )

    private fun onChangeValueForMainTab(vendor: VendorEssentialsUi) {
        val title = "*Поставщик ${vendor.displayName}"
        val navTabState = MainTabComponent.NavTabState(title)
        _model.update { navTabState }
    }

    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Child {
        return when (config) {
            is Config.Create -> Child.Create(
                CreateVendorSingleLineComponent(
                    componentContext = componentContext,
                    onSuccessCreate = { stackNavigation.replaceAll(Config.Update(it)) },
                    componentFactory = essentialsComponentFactory,
                    createVendorRepository = scope.get(),
                    observeOnItem = { vendor -> onChangeValueForMainTab(vendor) }
                )
            )

            is Config.Update -> Child.Update(
                VendorFormTabsComponent(
                    componentContext = componentContext,
                    scope = scope,
                    vendorId = config.id,
                    closeFormScreen = closeTab,
                    componentFactory = essentialsComponentFactory,
                    observeOnVendor = ::onChangeValueForMainTab
                )
            )
        }
    }

    internal val stack: Value<ChildStack<Config, Child>> = childStack(
        source = stackNavigation,
        serializer = Config.serializer(),
        initialConfiguration = if (vendorId == 0) Config.Create else Config.Update(vendorId),
        handleBackButton = false,
        childFactory = ::createChild
    )

    @Serializable
    sealed interface Config {
        @Serializable
        data object Create : Config

        @Serializable
        data class Update(val id: Int) : Config
    }

    internal sealed class Child {
        class Create(val component: CreateVendorSingleLineComponent) : Child()
        class Update(val component: VendorFormTabsComponent) : Child()
    }
}
