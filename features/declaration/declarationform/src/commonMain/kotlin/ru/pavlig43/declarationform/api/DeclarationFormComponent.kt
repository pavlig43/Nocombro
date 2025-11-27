package ru.pavlig43.declarationform.api

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
import ru.pavlig43.declarationform.internal.component.CreateDeclarationComponent
import ru.pavlig43.declarationform.internal.component.DeclarationFormTabInnerTabsComponent
import ru.pavlig43.declarationform.internal.di.createDeclarationFormModule

class DeclarationFormComponent(
    declarationId: Int,
    val closeTab: () -> Unit,
    private val onOpenVendorTab: (Int) -> Unit,
    componentContext: ComponentContext,
    private val dependencies: IDeclarationDependencies,
) : ComponentContext by componentContext, SlotComponent {

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createDeclarationFormModule(dependencies))

    private val _model = MutableStateFlow(SlotComponent.TabModel(""))
    override val model = _model.asStateFlow()

    private val stackNavigation = StackNavigation<Config>()

    internal val stack: Value<ChildStack<Config, Child>> = childStack(
        source = stackNavigation,
        serializer = Config.serializer(),
        key = "declaration",
        initialConfiguration = if (declarationId == 0) Config.Create else Config.Update(
            declarationId
        ),
        handleBackButton = false,
        childFactory = ::createChild
    )


    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Child {
        return when (config) {
            is Config.Create -> Child.Create(
                CreateDeclarationComponent(
                    componentContext = componentContext,
                    createItemRepository = scope.get(),
                    onSuccessCreate = { stackNavigation.replaceAll(Config.Update(it)) },
                    onChangeValueForMainTab = { onChangeValueForMainTab("* $it") },
                    itemListDependencies = dependencies.itemListDependencies,
                    onOpenVendorTab = onOpenVendorTab,
                )
            )

            is Config.Update -> Child.Update(
                DeclarationFormTabInnerTabsComponent(
                    componentContext = childContext("declaration_form"),
                    scope = scope,
                    declarationId = config.id,
                    closeFormScreen = closeTab,
                    onChangeValueForMainTab = { onChangeValueForMainTab(it) },
                    onOpenVendorTab = onOpenVendorTab,
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
        class Create(val component: CreateDeclarationComponent) : Child()
        class Update(val component: DeclarationFormTabInnerTabsComponent) : Child()
    }
}
