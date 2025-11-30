package ru.pavlig43.manageitem.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.manageitem.api.ProductFactoryParam
import ru.pavlig43.manageitem.api.data.CreateEssentialsRepository
import ru.pavlig43.manageitem.internal.data.ProductEssentialsUi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class ProductComponent(
    componentContext: ComponentContext,
    param: ProductFactoryParam,
    createEssentialsRepository: CreateEssentialsRepository<Product>,
) : EssentialsComponent<Product, ProductEssentialsUi>(
    componentContext = componentContext,
    initItem = ProductEssentialsUi(),
    isValidValuesFactory = { displayName.isNotBlank() && type != null },
    mapperToDTO = {
        Product(
            displayName = displayName,
            type = type ?: throw IllegalArgumentException("Product type required"),
            createdAt = createdAt ?: Clock.System.now().toEpochMilliseconds(),
            comment = comment,
            id = 0
        )
    },
    onSuccessUpsert = param.onSuccessUpsert,
    vendorInfoForTabName = { product -> param.onChangeValueForMainTab("*(Продукт) ${product.displayName}") },
    upsertEssentialsRepository = createEssentialsRepository,
)
