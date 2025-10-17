package ru.pavlig43.productform.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.product.ProductDeclaration
import ru.pavlig43.database.data.product.ProductDeclarationOutWithNameAndVendor
import ru.pavlig43.declaration.api.component.DeclarationTabSlot
import ru.pavlig43.declaration.api.data.ProductDeclarationUi
import ru.pavlig43.declarationlist.internal.data.DeclarationListRepository
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository

class ProductDeclarationTabSlot(
    componentContext: ComponentContext,
    productId: Int,
    declarationListRepository: DeclarationListRepository,
    updateRepository: UpdateCollectionRepository<ProductDeclarationOutWithNameAndVendor, ProductDeclaration>,
    openDeclarationTab: (Int) -> Unit,

    ) : DeclarationTabSlot<ProductDeclarationOutWithNameAndVendor, ProductDeclaration>(
    componentContext = componentContext,
    productId = productId,
    declarationListRepository = declarationListRepository,
    updateRepository = updateRepository,
    openDeclarationTab = openDeclarationTab,
    mapper = { mapper(productId) },
), ProductTabSlot

private fun ProductDeclarationUi.mapper(productId: Int): ProductDeclaration {
    return ProductDeclaration(
        productId = productId,
        declarationId = declarationId,
        id = id
    )
}





