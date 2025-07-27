package ru.pavlig43.productform.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.scope.Scope
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.UTC
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.loadinitdata.api.component.LoadInitDataState
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository
import ru.pavlig43.manageitem.api.component.IManageBaseValueItemComponent
import ru.pavlig43.manageitem.api.component.ManageBaseValueItemComponent
import ru.pavlig43.manageitem.api.data.RequireValues
import ru.pavlig43.productform.api.IProductFormDependencies
import ru.pavlig43.productform.internal.di.createProductFormModule
import ru.pavlig43.upsertitem.api.component.ISaveItemComponent
import ru.pavlig43.upsertitem.api.component.SaveItemComponent
import ru.pavlig43.upsertitem.api.data.ItemsForUpsert
import ru.pavlig43.upsertitem.data.ISaveItemRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ProductFormComponent(
    productId: Int,
    private val closeTab: () -> Unit,
    componentContext: ComponentContext,
    dependencies: IProductFormDependencies,
) : ComponentContext by componentContext, IProductFormComponent, SlotComponent {

    private val coroutineScope = componentCoroutineScope()
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createProductFormModule(dependencies))


    private val saveRepository: ISaveItemRepository<Product> = scope.get()
    private val initBaseValuesRepository: IInitDataRepository<Product,RequireValues> = scope.get()

    override val manageBaseValuesOfComponent: IManageBaseValueItemComponent =
        ManageBaseValueItemComponent(
            componentContext = childContext("manageBaseValuesOfComponent"),
            typeVariantList = ProductType.entries,
            id = productId,
            initDataRepository = initBaseValuesRepository
        )

    //TODO c декларацией
    private val productsParamsValidValue: Flow<Boolean> = manageBaseValuesOfComponent.isValidAllValue

    private fun getProductsForSave(): ItemsForUpsert<Product> {
        val requireValues = manageBaseValuesOfComponent.requireValues.value

        val newProduct = createProduct(requireValues)
        val initRequireValues =
            when (val loadState = manageBaseValuesOfComponent.initComponent.loadState.value) {
                is LoadInitDataState.Success<RequireValues> -> loadState.data.copy(type = requireValues.type)
                else -> throw IllegalStateException("Рекомендуемые значения(Имя и тип) при начальной загрузки загрузились с ошибкой ")
            }
        val oldProduct = createProduct(initRequireValues)
        return ItemsForUpsert(
            newItem = newProduct,
            initItem = oldProduct,

            )
    }
    override val saveProductComponent: ISaveItemComponent<Product> =
        SaveItemComponent(
            componentContext = childContext("saveProductComponent"),
            isOtherValidValue = productsParamsValidValue,
            getItems = ::getProductsForSave,
            onSuccessAction = closeTab,
            saveItemRepository = saveRepository
        )

    override fun closeScreen() {
        closeTab()
    }

    @OptIn(ExperimentalTime::class)
    private fun createProduct(
        requireValues: RequireValues,
    ): Product {
        val product = Product(
            id = requireValues.id,
            declarationId = null,
            displayName = requireValues.name,
            type = requireValues.type as ProductType,
            createdAt = requireValues.createdAt ?: UTC(Clock.System.now().toEpochMilliseconds()),
        )
        return product
    }

    override val model = manageBaseValuesOfComponent.requireValues.map {
        val prefix = if (productId == 0) "* " else ""
        SlotComponent.TabModel("$prefix ${it.type?.displayName ?: ""} ${it.name}")
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        SlotComponent.TabModel("")
    )

}




