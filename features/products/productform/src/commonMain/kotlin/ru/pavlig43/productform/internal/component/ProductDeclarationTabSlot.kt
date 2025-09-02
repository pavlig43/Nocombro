package ru.pavlig43.productform.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductDeclaration
import ru.pavlig43.database.data.product.ProductDeclarationOutWithDocumentName
import ru.pavlig43.declaration.api.component.DeclarationTabSlot
import ru.pavlig43.declaration.api.data.DeclarationUi
import ru.pavlig43.itemlist.api.data.ItemListRepository
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository

class ProductDeclarationTabSlot(
    componentContext: ComponentContext,
    id: Int,
    onOpenDocumentTab: (Int) -> Unit,
    documentListRepository: ItemListRepository<Document, DocumentType>,
    updateRepository: UpdateCollectionRepository<ProductDeclarationOutWithDocumentName, ProductDeclaration>,
) : DeclarationTabSlot<ProductDeclarationOutWithDocumentName, ProductDeclaration>(
    componentContext = componentContext,
    id = id,
    updateRepository = updateRepository,
    documentListRepository = documentListRepository,
    openDocumentTab = onOpenDocumentTab,
    mapper = { toProductDeclaration(it) }
), ProductTabSlot

private fun DeclarationUi.toProductDeclaration(productId: Int): ProductDeclaration {
    return ProductDeclaration(
        parentId = productId,
        documentId = documentId,
        isActual = isActual,
        id = id
    )
}



