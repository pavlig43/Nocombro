package ru.pavlig43.document.internal.component.tabs.tabslot

import ru.pavlig43.core.FormTabChild


internal sealed interface DocumentTabChild: FormTabChild{
    class Essentials(override val component: DocumentEssentialComponent): DocumentTabChild
    class Files(override val component: DocumentFilesComponent): DocumentTabChild
}