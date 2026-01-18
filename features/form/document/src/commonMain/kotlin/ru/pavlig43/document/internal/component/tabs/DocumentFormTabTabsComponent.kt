package ru.pavlig43.document.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.operator.map
import org.koin.core.scope.Scope
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.document.internal.component.tabs.tabslot.DocumentFilesComponent
import ru.pavlig43.document.internal.component.tabs.tabslot.DocumentEssentialComponent
import ru.pavlig43.document.internal.component.tabs.tabslot.DocumentTabChild
import ru.pavlig43.document.internal.data.DocumentEssentialsUi
import ru.pavlig43.update.component.IItemFormTabsComponent
import ru.pavlig43.update.component.UpdateComponent
import ru.pavlig43.update.component.getDefaultUpdateComponent

internal class DocumentFormTabsComponent(
    componentContext: ComponentContext,
    essentialFactory: EssentialComponentFactory<Document, DocumentEssentialsUi>,
    closeFormScreen: () -> Unit,
    scope: Scope,
    documentId: Int
) : ComponentContext by componentContext,
    IItemFormTabsComponent<DocumentTab, DocumentTabChild> {

    override val transactionExecutor: TransactionExecutor = scope.get()


    override val tabNavigationComponent: TabNavigationComponent<DocumentTab, DocumentTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                DocumentTab.Essentials,
                DocumentTab.Files
            ),
            serializer = DocumentTab.serializer(),
            tabChildFactory = { context, tabConfig: DocumentTab, _: () -> Unit ->
                when (tabConfig) {

                    DocumentTab.Essentials -> DocumentTabChild.Essentials(
                        DocumentEssentialComponent(
                            componentContext = context,
                            componentFactory = essentialFactory,
                            documentId = documentId,
                            updateRepository = scope.get(),
                        )
                    )


                    DocumentTab.Files -> DocumentTabChild.Files(
                        DocumentFilesComponent(
                            documentId = documentId,
                            dependencies = scope.get(),
                            componentContext = context
                        )
                    )
                }

            },
        )



    override val updateComponent = getDefaultUpdateComponent(componentContext,closeFormScreen)
    
}

