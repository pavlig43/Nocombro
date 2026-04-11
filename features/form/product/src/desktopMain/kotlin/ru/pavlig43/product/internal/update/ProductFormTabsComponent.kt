package ru.pavlig43.product.internal.update

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
import ru.pavlig43.product.internal.di.SingleRepositoryType
import ru.pavlig43.product.internal.di.UpdateCollectionRepositoryType
import ru.pavlig43.product.internal.model.ProductEssentialsUi
import ru.pavlig43.product.internal.update.ProductTabChild.Composition
import ru.pavlig43.product.internal.update.ProductTabChild.Essentials
import ru.pavlig43.product.internal.update.ProductTabChild.Files
import ru.pavlig43.product.internal.update.ProductTabChild.SafetyStock
import ru.pavlig43.product.internal.update.tabs.ProductFilesComponent
import ru.pavlig43.product.internal.update.tabs.composition.CompositionComponent
import ru.pavlig43.product.internal.update.tabs.declaration.ProductDeclarationComponent
import ru.pavlig43.product.internal.update.tabs.essential.ProductUpdateSingleLineComponent
import ru.pavlig43.product.internal.update.tabs.safety.SafetyStockComponent
import ru.pavlig43.update.component.IItemFormTabsComponent
import ru.pavlig43.update.component.getDefaultUpdateComponent

@Suppress("LongParameterList")
/**
 * Корневой компонент вкладок формы продукта.
 *
 * Помимо обычной таб-навигации хранит последнее состояние вкладки "Основное",
 * чтобы другие вкладки могли читать уже введенные пользователем данные без
 * прямой зависимости от UI этой вкладки.
 *
 * В контексте парсинга деклараций это нужно для получения актуального имени
 * продукта: вкладка деклараций берет его через [getProductName], а не хранит
 * отдельную копию сама.
 */
internal class ProductFormTabsComponent(
    componentContext: ComponentContext,
    componentFactory: SingleLineComponentFactory<Product, ProductEssentialsUi>,
    tabOpener: TabOpener,
    scope: Scope,
    productId: Int,
    private val observeOnProduct: (ProductEssentialsUi) -> Unit,
) : ComponentContext by componentContext,
    IItemFormTabsComponent<ProductTab, ProductTabChild> {

    override val transactionExecutor: TransactionExecutor = scope.get()
    private val coroutineScope = componentCoroutineScope()
    private val productEssentials = MutableStateFlow(componentFactory.initItem)

    /**
     * Синхронизирует состояние вкладки "Основное" в двух направлениях:
     * наружу через уже существующий [observeOnProduct] и локально в
     * [productEssentials], чтобы другие вкладки могли читать последнюю версию
     * данных продукта.
     */
    private fun observeOnProductInfo(product: ProductEssentialsUi) {
        observeOnProduct(product)
        productEssentials.update { product }
    }


    override val tabNavigationComponent: TabNavigationComponent<ProductTab, ProductTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                ProductTab.Essentials,
                ProductTab.Files,
                ProductTab.SafetyStock,
                ProductTab.Declaration
            ),
            enableBackNavigation = false,
            serializer = ProductTab.serializer(),
            tabChildFactory = { context, tabConfig: ProductTab, _: () -> Unit ->
                when (tabConfig) {
                    is ProductTab.Essentials -> Essentials(
                        ProductUpdateSingleLineComponent(
                            componentContext = context,
                            productId = productId,
                            updateRepository = scope.get(SingleRepositoryType.ESSENTIALS.qualifier),
                            componentFactory = componentFactory,
                            observeOnItem = ::observeOnProductInfo,
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

                    ProductTab.Composition -> Composition(
                        CompositionComponent(
                            componentContext = context,
                            parentId = productId,
                            repository = scope.get(UpdateCollectionRepositoryType.Composition.qualifier),
                            immutableTableDependencies = scope.get(),
                            tabOpener = tabOpener
                        )
                    )

                    ProductTab.Declaration -> ProductTabChild.Declaration1(
                        ProductDeclarationComponent(
                            componentContext = context,
                            productId = productId,
                            repository = scope.get(),
                            tabOpener = tabOpener,
                            immutableTableDependencies = scope.get(),
                            getProductName = { productEssentials.value.displayName }
                        )
                    )

                    ProductTab.SafetyStock -> SafetyStock(
                        SafetyStockComponent(
                            componentContext = context,
                            productId = productId,
                            updateRepository = scope.get(SingleRepositoryType.SAFETY.qualifier),
                        )
                    )
                }
            },
        )

    private fun onSuccessInitData(product: ProductEssentialsUi) {
        observeOnProductInfo(product)
        coroutineScope.launch {
            if (product.productType == ProductType.FOOD_PF) {
                tabNavigationComponent.addTab(ProductTab.Composition)
            }
            tabNavigationComponent.onSelectTab(0)
        }
    }

    override val updateComponent = getDefaultUpdateComponent(componentContext)
}
