package ru.pavlig43.productform.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.addfile.api.component.UpdateFilesComponent
import ru.pavlig43.addfile.api.data.FileUi
import ru.pavlig43.database.data.product.ProductFile
import ru.pavlig43.update.data.UpdateCollectionRepository

internal class ProductFileTabSlot(
    componentContext: ComponentContext,
    productId: Int,
    updateRepository: UpdateCollectionRepository<ProductFile, ProductFile>
) : UpdateFilesComponent<ProductFile>(
    componentContext = componentContext,
    id = productId,
    updateRepository = updateRepository,
    mapper = { toFileData(it) }
), ProductTabSlot

private fun FileUi.toFileData(productId: Int): ProductFile {
    return ProductFile(
        productId = productId,
        path = path,
        id = id
    )
}

//internal class GenerateSpecification(
//    componentContext: ComponentContext,
//) : ComponentContext by componentContext {
//
//}