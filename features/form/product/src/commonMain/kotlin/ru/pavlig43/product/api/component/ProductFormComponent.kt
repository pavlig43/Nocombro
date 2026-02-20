package ru.pavlig43.product.api.component

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
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.product.api.ProductFormDependencies
import ru.pavlig43.product.internal.create.component.CreateProductSingleLineComponent
import ru.pavlig43.product.internal.di.createProductFormModule
import ru.pavlig43.product.internal.model.ProductEssentialsUi
import ru.pavlig43.product.internal.model.toUi
import ru.pavlig43.product.internal.update.ProductFormTabsComponent

class ProductFormComponent(
    productId: Int,
    private val tabOpener: TabOpener,
    componentContext: ComponentContext,
    dependencies: ProductFormDependencies,
) : ComponentContext by componentContext, MainTabComponent {

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createProductFormModule(dependencies))


    private val _model = MutableStateFlow(MainTabComponent.NavTabState(""))
    override val model = _model.asStateFlow()

    private val stackNavigation = StackNavigation<Config>()

    private val essentialsComponentFactory = SingleLineComponentFactory<Product, ProductEssentialsUi>(
        initItem = ProductEssentialsUi(),
        errorFactory = { item ->
            buildList {
                if (item.displayName.isBlank()) add("Название продукта обязательно")
                if (item.productType == null) add("Тип продукта обязателен")
            }
        },
        mapperToUi = { toUi() }
    )


    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Child {
        return when (config) {
            is Config.Create -> Child.Create(
                CreateProductSingleLineComponent(
                    componentContext = componentContext,
                    onSuccessCreate = { stackNavigation.replaceAll(Config.Update(it)) },
                    observeOnItem = { product -> onChangeValueForMainTab(product) },
                    componentFactory = essentialsComponentFactory,
                    createProductRepository = scope.get()
                )

            )

            is Config.Update -> Child.Update(
                ProductFormTabsComponent(
                    componentContext = componentContext,
                    componentFactory = essentialsComponentFactory,
                    tabOpener = tabOpener,
                    scope = scope,
                    productId = config.id,
                    observeOnProduct = ::onChangeValueForMainTab
                )
            )
        }
    }


    private fun onChangeValueForMainTab(product: ProductEssentialsUi) {
        val title = if (product.id == 0) {
            "* ${product.displayName}"
        } else {
            " ${product.displayName}"
        }
        val navTabState = MainTabComponent.NavTabState(title)
        _model.update { navTabState }
    }

    internal val stack: Value<ChildStack<Config, Child>> = childStack(
        source = stackNavigation,
        serializer = Config.serializer(),
        initialConfiguration = if (productId == 0) Config.Create else Config.Update(productId),
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
        class Create(val component: CreateProductSingleLineComponent) : Child()
        class Update(val component: ProductFormTabsComponent) : Child()
    }
}




