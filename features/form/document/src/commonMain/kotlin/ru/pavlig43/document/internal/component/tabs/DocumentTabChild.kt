package ru.pavlig43.document.internal.component.tabs

import ru.pavlig43.core.FormTabChild
import ru.pavlig43.document.internal.component.tabs.component.DocumentEssentialComponent
import ru.pavlig43.document.internal.component.tabs.component.DocumentFilesComponent

internal sealed interface DocumentTabChild: FormTabChild {
    class Essentials(override val component: DocumentEssentialComponent): DocumentTabChild
    class Files(override val component: DocumentFilesComponent): DocumentTabChild
}