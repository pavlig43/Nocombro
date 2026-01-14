package ru.pavlig43.product.internal.component.tabs.tabslot

import ru.pavlig43.core.FormTabChild


internal sealed interface ProductTabChild: FormTabChild{
    class Essentials(override val component: ProductEssentialsComponent): ProductTabChild
    class Files(override val component: ProductFilesComponent): ProductTabChild
    class Declaration(override val component: ProductDeclarationComponent): ProductTabChild
    class Composition(override val component: CompositionComponent): ProductTabChild
}