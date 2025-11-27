package ru.pavlig43.productform.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.form.api.component.UpdateItemSlotComponent
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.productform.internal.toProduct


internal class ProductRequiresTabSlot(
    componentContext: ComponentContext,
    productId: Int,
    updateRepository: IUpdateRepository<Product, Product>,
    onChangeValueForMainTab: (String) -> Unit
) : UpdateItemSlotComponent<Product, ProductType>(
    componentContext = componentContext,
    id = productId,
    typeVariantList = ProductType.entries,
    updateRepository = updateRepository,
    mapper = { toProduct() },
    onChangeValueForMainTab = onChangeValueForMainTab
), ProductTabSlot