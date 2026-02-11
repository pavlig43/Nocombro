package ru.pavlig43.declaration.internal.update

import ru.pavlig43.core.FormTabChild
import ru.pavlig43.declaration.internal.update.tabs.essential.DeclarationUpdateSingleLineComponent
import ru.pavlig43.declaration.internal.update.tabs.component.DeclarationFilesComponent

internal sealed interface DeclarationTabChild: FormTabChild {
    class Essential(override val component: DeclarationUpdateSingleLineComponent) : DeclarationTabChild
    class File(override val component: DeclarationFilesComponent) : DeclarationTabChild
}
