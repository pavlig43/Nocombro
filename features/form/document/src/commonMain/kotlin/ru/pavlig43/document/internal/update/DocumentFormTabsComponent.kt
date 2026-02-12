package ru.pavlig43.document.internal.update

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import org.koin.core.scope.Scope
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.document.internal.model.DocumentEssentialsUi
import ru.pavlig43.document.internal.update.DocumentTabChild.Essential
import ru.pavlig43.document.internal.update.DocumentTabChild.Files
import ru.pavlig43.document.internal.update.tabs.DocumentFilesComponent
import ru.pavlig43.document.internal.update.tabs.essential.DocumentUpdateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.update.component.IItemFormTabsComponent
import ru.pavlig43.update.component.getDefaultUpdateComponent

internal class DocumentFormTabsComponent(
    componentContext: ComponentContext,
    componentFactory: SingleLineComponentFactory<Document, DocumentEssentialsUi>,
    closeFormScreen: () -> Unit,
    scope: Scope,
    documentId: Int,
    observeOnDocument:(DocumentEssentialsUi)-> Unit,
) : ComponentContext by componentContext,
    IItemFormTabsComponent<DocumentTab, DocumentTabChild> {

    override val transactionExecutor: TransactionExecutor = scope.get()



    override val tabNavigationComponent: TabNavigationComponent<DocumentTab, DocumentTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                DocumentTab.Essential,
                DocumentTab.Files
            ),
            serializer = DocumentTab.serializer(),
            tabChildFactory = { context, tabConfig: DocumentTab, _: () -> Unit ->
                when (tabConfig) {

                    DocumentTab.Files -> Files(
                        DocumentFilesComponent(
                            documentId = documentId,
                            dependencies = scope.get(),
                            componentContext = context
                        )
                    )

                    DocumentTab.Essential -> Essential(
                        DocumentUpdateSingleLineComponent(
                            componentContext = context,
                            documentId = documentId,
                            updateRepository = scope.get(),
                            observeOnItem = observeOnDocument,
                            onSuccessInitData = observeOnDocument,
                            componentFactory = componentFactory
                        )
                    )
                }

            },
        )


    override val updateComponent = getDefaultUpdateComponent(componentContext,closeFormScreen)

}