package ru.pavlig43.product.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.launch
import org.koin.core.qualifier.qualifier
import org.koin.core.scope.Scope
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.product.internal.component.tabs.component.ProductDeclarationComponent
import ru.pavlig43.product.internal.component.tabs.component.ProductEssentialsComponent
import ru.pavlig43.product.internal.component.tabs.component.ProductFilesComponent
import ru.pavlig43.product.internal.component.tabs.component.composition.CompositionComponent
import ru.pavlig43.product.internal.data.ProductEssentialsUi
import ru.pavlig43.product.internal.di.UpdateCollectionRepositoryType
import ru.pavlig43.update.component.IItemFormTabsComponent
import ru.pavlig43.update.component.getDefaultUpdateComponent

@Suppress("LongParameterList")
internal class ProductFormTabsComponent(
    componentContext: ComponentContext,
    componentFactory: EssentialComponentFactory<Product, ProductEssentialsUi>,
    closeFormScreen: () -> Unit,
    tabOpener: TabOpener,
    scope: Scope,
    productId: Int,
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
            tabChildFactory = { context, tabConfig: ProductTab, closeTab: () -> Unit ->
                when (tabConfig) {

                    is ProductTab.Essentials -> ProductTabChild.Essentials(
                        ProductEssentialsComponent(
                            componentContext = context,
                            productId = productId,
                            updateRepository = scope.get(),
                            componentFactory = componentFactory,
                            onSuccessInitData = {product->
                                coroutineScope.launch {
                                    if (product.productType is ProductType.Food.Pf){
                                        tabNavigationComponent.addTab(ProductTab.Composition)
                                    }
                                }

                            }
                        )
                    )


                    is ProductTab.Files -> ProductTabChild.Files(
                        ProductFilesComponent(
                            productId = productId,
                            dependencies = scope.get(),
                            componentContext = context
                        )
                    )

                    is ProductTab.Declaration -> ProductTabChild.Declaration(
                        ProductDeclarationComponent(
                            componentContext = context,
                            productId = productId,
                            updateRepository = scope.get(UpdateCollectionRepositoryType.Declaration.qualifier),
                            tabOpener = tabOpener,
                            dependencies = scope.get()
                        )
                    )


                    ProductTab.Composition -> ProductTabChild.Composition(
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
    override val updateComponent = getDefaultUpdateComponent(componentContext,closeFormScreen)


}