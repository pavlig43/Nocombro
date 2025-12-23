package ru.pavlig43.product.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.operator.map
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.product.internal.component.tabs.tabslot.*
import ru.pavlig43.product.internal.data.ProductEssentialsUi
import ru.pavlig43.product.internal.di.UpdateCollectionRepositoryType
import ru.pavlig43.update.component.IItemFormInnerTabsComponent
import ru.pavlig43.update.component.UpdateComponent

@Suppress("LongParameterList")
internal class ProductFormTabInnerTabsComponent(
    componentContext: ComponentContext,
    componentFactory: EssentialComponentFactory<Product, ProductEssentialsUi>,
    closeFormScreen:()->Unit,
    onOpenDeclarationTab:(Int)->Unit,
    onOpenProductTab:(Int)->Unit,
    scope: Scope,
    productId: Int,
) : ComponentContext by componentContext,
    IItemFormInnerTabsComponent<ProductTab, ProductTabSlot> {

    private val dbTransaction: DataBaseTransaction = scope.get()


    override val tabNavigationComponent: TabNavigationComponent<ProductTab, ProductTabSlot> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                ProductTab.Essentials,
                ProductTab.Files,
                ProductTab.Declaration,
                ProductTab.Ingredients
            ),
            serializer = ProductTab.serializer(),
            slotFactory = { context, tabConfig: ProductTab, _: () -> Unit ->
                when (tabConfig) {

                    ProductTab.Essentials -> EssentialTabSlot(
                        componentContext = context,
                        productId = productId,
                        updateRepository = scope.get(),
                        componentFactory = componentFactory
                    )


                    ProductTab.Files -> ProductFileTabSlot(
                        productId = productId,
                        updateRepository = scope.get(named(UpdateCollectionRepositoryType.Files.name)),
                        componentContext = context
                    )

                    ProductTab.Declaration -> ProductDeclarationTabSlot(
                        componentContext = context,
                        productId = productId,
                        updateRepository = scope.get(named(UpdateCollectionRepositoryType.Declaration.name)),
                        openDeclarationTab = onOpenDeclarationTab,
                        dependencies = scope.get()
                    )

                    ProductTab.Ingredients -> CompositionTabSlot(
                        componentContext = componentContext,
                        productId = productId,
                        openProductTab = onOpenProductTab,
                        updateCompositionRepository = scope.get(named(UpdateCollectionRepositoryType.Composition.name)),
                        dependencies = scope.get(),
                    )
                }

            },
        )
    private suspend fun update(): Result<Unit> {
        val blocks = tabNavigationComponent.children.map { children->
            children.items.map { child-> suspend {child.instance.onUpdate()} } }
            return  dbTransaction.transaction(blocks.value)
    }
    override val updateComponent: UpdateComponent = UpdateComponent(
        componentContext = childContext("update"),
        onUpdateComponent = { update() },
        closeFormScreen = closeFormScreen
    )



}