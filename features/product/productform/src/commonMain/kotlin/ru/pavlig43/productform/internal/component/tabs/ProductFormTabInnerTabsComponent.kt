package ru.pavlig43.productform.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.operator.map
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.tabs.DefaultTabNavigationComponent
import ru.pavlig43.core.tabs.ITabNavigationComponent
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.form.api.component.IItemFormInnerTabsComponent
import ru.pavlig43.manageitem.internal.component.EssentialComponentFactory
import ru.pavlig43.productform.internal.component.tabs.tabslot.*
import ru.pavlig43.productform.internal.data.ProductEssentialsUi
import ru.pavlig43.productform.internal.di.UpdateCollectionRepositoryType
import ru.pavlig43.upsertitem.api.component.UpdateComponent

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


    override val tabNavigationComponent: ITabNavigationComponent<ProductTab, ProductTabSlot> =
        DefaultTabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                ProductTab.Essentials,
                ProductTab.Files,
                ProductTab.Declaration,
                ProductTab.Ingredients
            ),
            serializer = ProductTab.serializer(),
            slotFactory = { context, tabConfig: ProductTab, _: (ProductTab) -> Unit, _: () -> Unit ->
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
                        itemListDependencies = scope.get()
                    )

                    ProductTab.Ingredients -> CompositionTabSlot(
                        componentContext = componentContext,
                        productId = productId,
                        openProductTab = onOpenProductTab,
                        updateCompositionRepository = scope.get(named(UpdateCollectionRepositoryType.Composition.name)),
                        itemListDependencies = scope.get(),
                    )
                }

            },
        )
    private suspend fun update(): RequestResult<Unit> {
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