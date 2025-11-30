package ru.pavlig43.productform.api.component

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
import ru.pavlig43.manageitem.api.ProductFactoryParam
import ru.pavlig43.manageitem.api.component.UpsertEssentialsFactoryComponent
import ru.pavlig43.productform.api.ProductFormDependencies
import ru.pavlig43.productform.internal.component.ProductFormTabInnerTabsComponent
import ru.pavlig43.productform.internal.di.createProductFormModule

class ProductFormComponent(
    private val productId: Int,
    val closeTab: () -> Unit,
    private val onOpenProductTab: (Int) -> Unit,
    private val onOpenDeclarationTab: (Int) -> Unit,
    componentContext: ComponentContext,
    dependencies: ProductFormDependencies,
) : ComponentContext by componentContext, SlotComponent {

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createProductFormModule(dependencies))


    private val _model = MutableStateFlow(SlotComponent.TabModel(""))
    override val model = _model.asStateFlow()

    private val stackNavigation = StackNavigation<Config>()

    internal val stack: Value<ChildStack<Config, Child>> = childStack(
        source = stackNavigation,
        serializer = Config.serializer(),
        initialConfiguration = if (productId == 0) Config.Create else Config.Update(productId),
        handleBackButton = false,
        childFactory = ::createChild
    )


    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Child {
        return when (config) {
            is Config.Create -> Child.Create(
                UpsertEssentialsFactoryComponent(
                    componentContext = componentContext,
                    upsertEssentialsFactoryParam = ProductFactoryParam(
                        upsertEssentialsDependencies = scope.get(),
                        onSuccessUpsert = {stackNavigation.replaceAll(Config.Update(it))},
                        onChangeValueForMainTab = ::onChangeValueForMainTab,
                        id = productId
                    ),
                    upsertEssentialsDependencies = scope.get(),
                )

            )

            is Config.Update -> Child.Update(
                ProductFormTabInnerTabsComponent(
                    componentContext = childContext("product_form"),
                    scope = scope,
                    productId = config.id,
                    closeFormScreen = closeTab,
                    onOpenDeclarationTab = onOpenDeclarationTab,
                    onOpenProductTab = onOpenProductTab,
                    onChangeValueForMainTab = {onChangeValueForMainTab(it)}
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
        class Create(val component: UpsertEssentialsFactoryComponent) : Child()
        class Update(val component: ProductFormTabInnerTabsComponent) : Child()
    }
}




