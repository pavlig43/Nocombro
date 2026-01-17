package ru.pavlig43.product.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.operator.map
import org.koin.core.qualifier.qualifier
import org.koin.core.scope.Scope
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.product.internal.component.tabs.tabslot.CompositionComponent
import ru.pavlig43.product.internal.component.tabs.tabslot.ProductDeclarationComponent
import ru.pavlig43.product.internal.component.tabs.tabslot.ProductEssentialsComponent
import ru.pavlig43.product.internal.component.tabs.tabslot.ProductFilesComponent
import ru.pavlig43.product.internal.component.tabs.tabslot.ProductTabChild
import ru.pavlig43.product.internal.data.ProductEssentialsUi
import ru.pavlig43.product.internal.di.UpdateCollectionRepositoryType
import ru.pavlig43.update.component.IItemFormInnerTabsComponent
import ru.pavlig43.update.component.UpdateComponent

@Suppress("LongParameterList")
internal class ProductFormTabInnerTabsComponent(
    componentContext: ComponentContext,
    componentFactory: EssentialComponentFactory<Product, ProductEssentialsUi>,
    closeFormScreen: () -> Unit,
    onOpenDeclarationTab: (Int) -> Unit,
    onOpenProductTab: (Int) -> Unit,
    scope: Scope,
    productId: Int,
) : ComponentContext by componentContext,
    IItemFormInnerTabsComponent<ProductTab, ProductTabChild> {

    private val dbTransaction: TransactionExecutor = scope.get()


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

    private suspend fun update(): Result<Unit> {
        val blocks = tabNavigationComponent.tabChildren.map { children ->
            children.items.map { child -> suspend { child.instance.component.onUpdate() } }
        }
        return dbTransaction.transaction(blocks.value)
    }



    override val updateComponent: UpdateComponent = UpdateComponent(
        componentContext = childContext("update"),
        onUpdateComponent = { update() },
        errorMessages = getErrors(lifecycle),
        closeFormScreen = closeFormScreen
    )

}