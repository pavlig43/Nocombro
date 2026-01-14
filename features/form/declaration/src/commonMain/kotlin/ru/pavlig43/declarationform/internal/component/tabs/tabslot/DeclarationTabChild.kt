package ru.pavlig43.declarationform.internal.component.tabs.tabslot

import ru.pavlig43.core.FormTabChild

internal sealed interface DeclarationTabChild: FormTabChild{
    class Essential(override val component: DeclarationEssentialComponent): DeclarationTabChild
    class File (override val component: DeclarationFilesComponent): DeclarationTabChild
}