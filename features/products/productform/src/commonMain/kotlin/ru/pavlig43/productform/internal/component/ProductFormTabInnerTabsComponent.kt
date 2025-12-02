package ru.pavlig43.productform.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.tabs.DefaultTabNavigationComponent
import ru.pavlig43.core.tabs.ITabNavigationComponent
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.form.api.component.IItemFormInnerTabsComponent
import ru.pavlig43.productform.internal.di.UpdateCollectionRepositoryType
import ru.pavlig43.productform.internal.di.UpdateRepositoryType
import ru.pavlig43.upsertitem.api.component.UpdateComponent

@Suppress("LongParameterList")
internal class ProductFormTabInnerTabsComponent(
    componentContext: ComponentContext,
    closeFormScreen:()->Unit,
    onOpenDeclarationTab:(Int)->Unit,
    onOpenProductTab:(Int)->Unit,
    scope: Scope,
    productId: Int,
    onChangeValueForMainTab: (String) -> Unit
) : ComponentContext by componentContext,
    IItemFormInnerTabsComponent<ProductTab, ProductTabSlot> {

    private val _mainTabTitle = MutableStateFlow("")
    private val dbTransaction: DataBaseTransaction = scope.get()


    override val tabNavigationComponent: ITabNavigationComponent<ProductTab, ProductTabSlot> =
        DefaultTabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                ProductTab.RequireValues,
                ProductTab.Files,
                ProductTab.Declaration,
                ProductTab.Ingredients
            ),
            serializer = ProductTab.serializer(),
            slotFactory = { context, tabConfig: ProductTab, _: (ProductTab) -> Unit, _: () -> Unit ->
                when (tabConfig) {

                    ProductTab.RequireValues ->ProductRequiresTabSlot(
                        componentContext = context,
                        productId = productId,
                        updateRepository = scope.get(named(UpdateRepositoryType.Product.name)),
                        onChangeValueForMainTab = onChangeValueForMainTab
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
        val blocks: Value<List<suspend () -> Unit>> = tabNavigationComponent.children.map { children->
            children.items.map { child-> suspend {child.instance.onUpdate()} } }
        return dbSafeCall("product form"){
            dbTransaction.transaction(blocks.value)
        }
    }
    override val updateComponent: UpdateComponent = UpdateComponent(
        componentContext = childContext("update"),
        onUpdateComponent = {update()},
        closeFormScreen = closeFormScreen
    )



}
