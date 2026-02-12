package ru.pavlig43.document.internal.update

import ru.pavlig43.core.FormTabChild
import ru.pavlig43.document.internal.update.tabs.DocumentFilesComponent
import ru.pavlig43.document.internal.update.tabs.essential.DocumentUpdateSingleLineComponent

internal sealed interface DocumentTabChild: FormTabChild {
    class Essential(override val component: DocumentUpdateSingleLineComponent): DocumentTabChild
    class Files(override val component: DocumentFilesComponent): DocumentTabChild
}