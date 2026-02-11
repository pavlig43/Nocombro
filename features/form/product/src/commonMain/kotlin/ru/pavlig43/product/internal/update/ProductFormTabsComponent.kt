package ru.pavlig43.product.internal.update

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.launch
import org.koin.core.qualifier.qualifier
import org.koin.core.scope.Scope
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.product.internal.di.UpdateCollectionRepositoryType
import ru.pavlig43.product.internal.model.ProductEssentialsUi
import ru.pavlig43.product.internal.update.ProductTabChild.*
import ru.pavlig43.product.internal.update.tabs.ProductDeclarationComponent
import ru.pavlig43.product.internal.update.tabs.ProductFilesComponent
import ru.pavlig43.product.internal.update.tabs.composition.CompositionComponent
import ru.pavlig43.product.internal.update.tabs.essential.ProductUpdateSingleLineComponent
import ru.pavlig43.update.component.IItemFormTabsComponent
import ru.pavlig43.update.component.getDefaultUpdateComponent

@Suppress("LongParameterList")
internal class ProductFormTabsComponent(
    componentContext: ComponentContext,
    componentFactory: SingleLineComponentFactory<Product, ProductEssentialsUi>,
    closeFormScreen: () -> Unit,
    tabOpener: TabOpener,
    scope: Scope,
    productId: Int,
    private val observeOnProduct: (ProductEssentialsUi) -> Unit,
) : ComponentContext by componentContext,
    IItemFormTabsComponent<ProductTab, ProductTabChild> {

    override val transactionExecutor: TransactionExecutor = scope.get()
    private val coroutineScope = componentCoroutineScope()

    override val tabNavigationComponent: TabNavigationComponent<ProductTab, ProductTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                ProductTab.Essentials,
                ProductTab.Files,
                ProductTab.Declaration,
            ),
            serializer = ProductTab.serializer(),
            tabChildFactory = { context, tabConfig: ProductTab, _: () -> Unit ->
                when (tabConfig) {

                    is ProductTab.Essentials -> Essentials(
                        ProductUpdateSingleLineComponent(
                            componentContext = context,
                            productId = productId,
                            updateRepository = scope.get(),
                            componentFactory = componentFactory,
                            observeOnItem = observeOnProduct,
                            onSuccessInitData = ::onSuccessInitData
                        )
                    )

                    is ProductTab.Files -> Files(
                        ProductFilesComponent(
                            productId = productId,
                            dependencies = scope.get(),
                            componentContext = context
                        )
                    )

                    is ProductTab.Declaration -> Declaration(
                        ProductDeclarationComponent(
                            componentContext = context,
                            productId = productId,
                            updateRepository = scope.get(UpdateCollectionRepositoryType.Declaration.qualifier),
                            tabOpener = tabOpener,
                            dependencies = scope.get()
                        )
                    )

                    ProductTab.Composition -> Composition(
                        CompositionComponent(
                            componentContext = context,
                            parentId = productId,
                            repository = scope.get(UpdateCollectionRepositoryType.Composition.qualifier),
                            immutableTableDependencies = scope.get(),
                            tabOpener = tabOpener
                        )
                    )
                }

            },
        )
    private fun onSuccessInitData(product: ProductEssentialsUi){
        observeOnProduct(product)
        coroutineScope.launch {
            if (product.productType == ProductType.FOOD_PF){
                tabNavigationComponent.addTab(ProductTab.Composition)
            }
            tabNavigationComponent.onSelectTab(0)
        }

    }

    override val updateComponent = getDefaultUpdateComponent(componentContext, closeFormScreen)
}
