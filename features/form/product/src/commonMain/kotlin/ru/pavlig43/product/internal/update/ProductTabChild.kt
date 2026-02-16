package ru.pavlig43.product.internal.update

import ru.pavlig43.core.FormTabChild
import ru.pavlig43.product.internal.update.tabs.ProductFilesComponent
import ru.pavlig43.product.internal.update.tabs.composition.CompositionComponent
import ru.pavlig43.product.internal.update.tabs.declaration.ProductDeclarationComponent
import ru.pavlig43.product.internal.update.tabs.essential.ProductUpdateSingleLineComponent

internal sealed interface ProductTabChild : FormTabChild {
    class Essentials(override val component: ProductUpdateSingleLineComponent) : ProductTabChild
    class Files(override val component: ProductFilesComponent) : ProductTabChild

    class Declaration1(override val component: ProductDeclarationComponent) : ProductTabChild
    class Composition(override val component: CompositionComponent) : ProductTabChild
}
