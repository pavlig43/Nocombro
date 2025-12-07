package ru.pavlig43.product.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.product.ProductDeclaration
import ru.pavlig43.database.data.product.ProductDeclarationOut
import ru.pavlig43.declaration.api.component.DeclarationTabSlot
import ru.pavlig43.declaration.api.data.ItemDeclarationUi
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.update.data.UpdateCollectionRepository

class ProductDeclarationTabSlot(
    componentContext: ComponentContext,
    productId: Int,
    itemListDependencies: ItemListDependencies,
    updateRepository: UpdateCollectionRepository<ProductDeclarationOut, ProductDeclaration>,
    openDeclarationTab: (Int) -> Unit,

    ) : DeclarationTabSlot<ProductDeclarationOut, ProductDeclaration>(
    componentContext = componentContext,
    productId = productId,
    updateRepository = updateRepository,
    openDeclarationTab = openDeclarationTab,
    itemListDependencies = itemListDependencies,
    mapper = { mapper(productId) },
), ProductTabSlot

private fun ItemDeclarationUi.mapper(productId: Int): ProductDeclaration {
    return ProductDeclaration(
        productId = productId,
        declarationId = declarationId,
        id = id
    )
}





