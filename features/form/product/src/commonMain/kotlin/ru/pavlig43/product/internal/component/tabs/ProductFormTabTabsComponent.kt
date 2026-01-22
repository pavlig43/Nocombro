package ru.pavlig43.product.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import org.koin.core.qualifier.qualifier
import org.koin.core.scope.Scope
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.product.internal.component.tabs.component.composition.CompositionComponent
import ru.pavlig43.product.internal.component.tabs.component.ProductDeclarationComponent
import ru.pavlig43.product.internal.component.tabs.component.ProductEssentialsComponent
import ru.pavlig43.product.internal.component.tabs.component.ProductFilesComponent
import ru.pavlig43.product.internal.data.ProductEssentialsUi
import ru.pavlig43.product.internal.di.UpdateCollectionRepositoryType
import ru.pavlig43.update.component.IItemFormTabsComponent
import ru.pavlig43.update.component.getDefaultUpdateComponent

@Suppress("LongParameterList")
internal class ProductFormTabsComponent(
    componentContext: ComponentContext,
    componentFactory: EssentialComponentFactory<Product, ProductEssentialsUi>,
    closeFormScreen: () -> Unit,
    onOpenDeclarationTab: (Int) -> Unit,
    onOpenProductTab: (Int) -> Unit,
    scope: Scope,
    productId: Int,
) : ComponentContext by componentContext,
    IItemFormTabsComponent<ProductTab, ProductTabChild> {

    override val transactionExecutor: TransactionExecutor = scope.get()


    override val tabNavigationComponent: TabNavigationComponent<ProductTab, ProductTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                ProductTab.Essentials,
                ProductTab.Files,
                ProductTab.Declaration,
                ProductTab.Composition
            ),
            serializer = ProductTab.serializer(),
            tabChildFactory = { context, tabConfig: ProductTab, closeTab: () -> Unit ->
                when (tabConfig) {

                    ProductTab.Essentials -> ProductTabChild.Essentials(
                        ProductEssentialsComponent(
                            componentContext = context,
                            productId = productId,
                            updateRepository = scope.get(),
                            componentFactory = componentFactory
                        )
                    )


                    ProductTab.Files -> ProductTabChild.Files(
                        ProductFilesComponent(
                            productId = productId,
                            dependencies = scope.get(),
                            componentContext = context
                        )
                    )

                    ProductTab.Declaration -> ProductTabChild.Declaration(
                        ProductDeclarationComponent(
                            componentContext = context,
                            productId = productId,
                            updateRepository = scope.get(UpdateCollectionRepositoryType.Declaration.qualifier),
                            openDeclarationTab = onOpenDeclarationTab,
                            dependencies = scope.get()
                        )
                    )


                    ProductTab.Composition -> ProductTabChild.Composition(
                        CompositionComponent(
                            componentContext = context,
                            parentId = productId,
                            repository = scope.get(UpdateCollectionRepositoryType.Composition.qualifier),
                            immutableTableDependencies = scope.get(),
                            updateEssentialsRepository = scope.get(),
                            onCloseThisTab = closeTab,
                            onOpenProductTab = onOpenProductTab
                        )
                    )
                }

            },
        )
    override val updateComponent = getDefaultUpdateComponent(componentContext,closeFormScreen)

}