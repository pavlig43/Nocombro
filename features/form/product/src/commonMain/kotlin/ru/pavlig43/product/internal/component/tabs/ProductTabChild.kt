package ru.pavlig43.product.internal.component.tabs

import ru.pavlig43.core.FormTabChild
import ru.pavlig43.product.internal.component.tabs.component.composition.CompositionComponent
import ru.pavlig43.product.internal.component.tabs.component.ProductDeclarationComponent
import ru.pavlig43.product.internal.component.tabs.component.ProductEssentialsComponent
import ru.pavlig43.product.internal.component.tabs.component.ProductFilesComponent

internal sealed interface ProductTabChild: FormTabChild {
    class Essentials(override val component: ProductEssentialsComponent): ProductTabChild
    class Files(override val component: ProductFilesComponent): ProductTabChild
    class Declaration(override val component: ProductDeclarationComponent): ProductTabChild
    class Composition(override val component: CompositionComponent): ProductTabChild
}