package ru.pavlig43.vendor.api

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
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
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.manageitem.api.VendorFactoryParam
import ru.pavlig43.manageitem.api.component.UpsertEssentialsFactoryComponent
import ru.pavlig43.vendor.api.VendorFormComponent.Child.Update
import ru.pavlig43.vendor.internal.component.VendorFormTabInnerTabsComponent
import ru.pavlig43.vendor.internal.di.createVendorFormModule

class VendorFormComponent(
    private val vendorId: Int,
    val closeTab: () -> Unit,
    componentContext: ComponentContext,
    dependencies: VendorFormDependencies,
) : ComponentContext by componentContext, SlotComponent {

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createVendorFormModule(dependencies))


    private val _model = MutableStateFlow(SlotComponent.TabModel(""))
    override val model = _model.asStateFlow()

    private val stackNavigation = StackNavigation<Config>()

    internal val stack: Value<ChildStack<Config, Child>> = childStack(
        source = stackNavigation,
        serializer = Config.serializer(),
        initialConfiguration = if (vendorId == 0) Config.Create else Config.Update(vendorId),
        handleBackButton = false,
        childFactory = ::createChild
    )


    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Child {
        return when (config) {
            Config.Create -> Child.Create(
                UpsertEssentialsFactoryComponent(
                    componentContext = componentContext,
                    upsertEssentialsDependencies = scope.get(),
                    upsertEssentialsFactoryParam = VendorFactoryParam(
                        upsertEssentialsDependencies = scope.get(),
                        onSuccessUpsert = {stackNavigation.replaceAll(Config.Update(it))},
                        onChangeValueForMainTab = ::onChangeValueForMainTab,
                        id = vendorId
                    )
                )
            )


            is Config.Update -> Update(
                VendorFormTabInnerTabsComponent(
                    componentContext = childContext("vendor_form"),
                    closeFormScreen = closeTab,
                    scope = scope,
                    vendorId = config.id,
                    onChangeValueForMainTab = { onChangeValueForMainTab(it) }
                )
            )


        }
    }


    private fun onChangeValueForMainTab(title: String) {

        val tabModel = SlotComponent.TabModel(title)
        _model.update { tabModel }
    }




    @Serializable
    sealed interface Config {
        @Serializable
        data object Create : Config

        @Serializable
        data class Update(val id: Int) : Config
    }

    internal sealed class Child {
        class Update(val component: VendorFormTabInnerTabsComponent) : Child()
        class Create(val component: UpsertEssentialsFactoryComponent) : Child()
    }
}
