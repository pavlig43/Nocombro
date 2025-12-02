package ru.pavlig43.productform.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.product.ProductDeclaration
import ru.pavlig43.database.data.product.ProductDeclarationOutWithNameAndVendor
import ru.pavlig43.declaration.api.component.DeclarationTabSlot
import ru.pavlig43.declaration.api.data.ProductDeclarationUi
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository

class ProductDeclarationTabSlot(
    componentContext: ComponentContext,
    productId: Int,
    itemListDependencies: ItemListDependencies,
    updateRepository: UpdateCollectionRepository<ProductDeclarationOutWithNameAndVendor, ProductDeclaration>,
    openDeclarationTab: (Int) -> Unit,

    ) : DeclarationTabSlot<ProductDeclarationOutWithNameAndVendor, ProductDeclaration>(
    componentContext = componentContext,
    productId = productId,
    updateRepository = updateRepository,
    openDeclarationTab = openDeclarationTab,
    itemListDependencies = itemListDependencies,
    mapper = { mapper(productId) },
), ProductTabSlot

private fun ProductDeclarationUi.mapper(productId: Int): ProductDeclaration {
    return ProductDeclaration(
        productId = productId,
        declarationId = declarationId,
        id = id
    )
}





