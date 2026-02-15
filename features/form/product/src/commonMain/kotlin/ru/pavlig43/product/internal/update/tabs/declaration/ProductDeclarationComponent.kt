package ru.pavlig43.product.internal.update.tabs.declaration

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductDeclarationOut
import ru.pavlig43.flowImmutable.api.component.FlowMultilineComponent
import ru.pavlig43.flowImmutable.api.data.FlowMultilineRepository
import kotlin.time.ExperimentalTime

internal class ProductDeclarationComponent(
    componentContext: ComponentContext,
    productId: Int,
    observableRepository: FlowMultilineRepository<ProductDeclarationIn, ProductDeclarationOut>,
): FlowMultilineComponent<ProductDeclarationOut, ProductDeclarationIn, ProductDeclarationTableUi, ProductDeclarationField>(
    componentContext = componentContext,
    parentId = productId,
    getObservableId = { it.declarationId },
    mapper = { toUi(it) },
    repository = observableRepository,
    filterMatcher =
    sortMatcher =
) {
}
@OptIn(ExperimentalTime::class)
private fun ProductDeclarationOut.toUi(composeKey: Int): ProductDeclarationTableUi {
    return ProductDeclarationTableUi(
        id = id,
        declarationId = declarationId,
        isActual = isActual,
        composeId = composeKey,
        declarationName = declarationName,
        vendorName = vendorName,
        displayName = displayName,
    )
}

private fun ProductDeclarationTableUi.mapper(productId: Int): ProductDeclarationIn {
    return ProductDeclarationIn(
        productId = productId,
        declarationId = declarationId,
        id = id
    )
}