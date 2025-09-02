package ru.pavlig43.productform.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.tabs.DefaultTabNavigationComponent
import ru.pavlig43.core.tabs.ITabNavigationComponent
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.form.api.component.IItemFormInnerTabsComponent
import ru.pavlig43.productform.internal.di.UpdateCollectionRepositoryType
import ru.pavlig43.productform.internal.di.UpdateRepositoryType
import ru.pavlig43.upsertitem.api.component.IUpdateComponent
import ru.pavlig43.upsertitem.api.component.UpdateComponent


internal class ProductFormTabInnerTabsComponent(
    componentContext: ComponentContext,
    closeFormScreen:()->Unit,
    onOpenDocumentTab:(Int)->Unit,
    scope: Scope,
    productId: Int,
    onChangeValueForMainTab: (String) -> Unit
) : ComponentContext by componentContext,
    IItemFormInnerTabsComponent<ProductTab, ProductTabSlot> {

    private val coroutineScope = componentCoroutineScope()
    private val _mainTabTitle = MutableStateFlow("")
    override val mainTabTitle: StateFlow<String> = _mainTabTitle.asStateFlow()
    private val dbTransaction: DataBaseTransaction = scope.get()


    override val tabNavigationComponent: ITabNavigationComponent<ProductTab, ProductTabSlot> =
        DefaultTabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                ProductTab.RequireValues,
                ProductTab.Files,
                ProductTab.Declaration
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
                        componentContext = componentContext,
                        id = productId,
                        updateRepository = scope.get(named(UpdateCollectionRepositoryType.Declaration.name)),
                        onOpenDocumentTab = onOpenDocumentTab,
                        documentListRepository = scope.get()
                    )
                }

            },
        )
    private suspend fun update(): RequestResult<Unit> {
        val blocks: Value<List<suspend () -> Unit>> = tabNavigationComponent.children.map { children->
            children.items.map { child-> suspend {child.instance.onUpdate()} } }
        return dbSafeCall("document form"){
            dbTransaction.transaction(blocks.value)
        }
    }
    override val updateComponent: IUpdateComponent = UpdateComponent(
        componentContext = childContext("update"),
        onUpdateComponent = {update()},
        closeFormScreen = closeFormScreen
    )



}
