package ru.pavlig43.vendor.component

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
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.vendor.api.VendorFormDependencies
import ru.pavlig43.vendor.internal.component.CreateVendorComponent
import ru.pavlig43.vendor.internal.component.tabs.VendorFormTabInnerTabsComponent
import ru.pavlig43.vendor.internal.data.VendorEssentialsUi
import ru.pavlig43.vendor.internal.data.toUi
import ru.pavlig43.vendor.internal.di.createVendorFormModule

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


    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Child {
        return when (config) {
            Config.Create -> Child.Create(
                CreateVendorComponent(
                    componentContext = componentContext,
                    onSuccessCreate = { stackNavigation.replaceAll(Config.Update(it)) },
                    createRepository = scope.get(),
                    componentFactory = componentFactory
                )
            )


            is Config.Update -> Child.Update(
                VendorFormTabInnerTabsComponent(
                    componentContext = componentContext,
                    closeFormScreen = closeTab,
                    scope = scope,
                    vendorId = config.id,
                    componentFactory = componentFactory
                )
            )


        }
    }


    private fun onChangeValueForMainTab(title: String) {

        val navTabState = MainTabComponent.NavTabState(title)
        _model.update { navTabState }
    }

    private val componentFactory = EssentialComponentFactory<Vendor, VendorEssentialsUi>(
        initItem = VendorEssentialsUi(),
        isValidFieldsFactory = { displayName.isNotBlank() },
        mapperToUi = { toUi() },
        produceInfoForTabName = { onChangeValueForMainTab("Поставщик ${it.displayName}") }
    )
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
        class Update(val component: VendorFormTabInnerTabsComponent) : Child()
        class Create(val component: CreateVendorComponent) : Child()
    }
}