package ru.pavlig43.declaration.internal.component.tabs

import ru.pavlig43.core.FormTabChild
import ru.pavlig43.declaration.internal.component.tabs.component.DeclarationEssentialComponent
import ru.pavlig43.declaration.internal.component.tabs.component.DeclarationFilesComponent

internal sealed interface DeclarationTabChild: FormTabChild {
    class Essential(override val component: DeclarationEssentialComponent): DeclarationTabChild
    class File (override val component: DeclarationFilesComponent): DeclarationTabChild
}