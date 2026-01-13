package ru.pavlig43.document.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.operator.map
import org.koin.core.scope.Scope
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.document.internal.component.tabs.tabslot.DocumentFileTabSlot
import ru.pavlig43.document.internal.component.tabs.tabslot.DocumentTabSlot
import ru.pavlig43.document.internal.component.tabs.tabslot.EssentialTabSlot
import ru.pavlig43.document.internal.data.DocumentEssentialsUi
import ru.pavlig43.update.component.IItemFormInnerTabsComponent
import ru.pavlig43.update.component.UpdateComponent

internal class DocumentFormTabInnerTabsComponent(
    componentContext: ComponentContext,
    essentialFactory: EssentialComponentFactory<Document, DocumentEssentialsUi>,
    closeFormScreen:()->Unit,
    scope: Scope,
    documentId: Int
) : ComponentContext by componentContext, IItemFormInnerTabsComponent<DocumentTab, DocumentTabSlot> {

    private val dbTransaction:DataBaseTransaction = scope.get()


    override val tabNavigationComponent: TabNavigationComponent<DocumentTab, DocumentTabSlot> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                DocumentTab.Essentials,
                DocumentTab.Files
            ),
            serializer = DocumentTab.serializer(),
            slotFactory = { context, tabConfig: DocumentTab,  _: () -> Unit ->
                when (tabConfig) {

                    DocumentTab.Essentials -> EssentialTabSlot(
                        componentContext = context,
                        componentFactory = essentialFactory,
                        documentId = documentId,
                        updateRepository = scope.get(),
                    )



                    DocumentTab.Files -> DocumentFileTabSlot(
                        documentId = documentId,
                        dependencies = scope.get(),
                        componentContext = context
                    )
                }

            },
        )
    private suspend fun update():Result<Unit> {
        val blocks= tabNavigationComponent.tabChildren.map { children->
            children.items.map { child-> suspend {child.instance.onUpdate()} } }
        return dbTransaction.transaction(blocks.value)

    }
    override val updateComponent: UpdateComponent = UpdateComponent(
        componentContext = childContext("update"),
        onUpdateComponent = {update()},
        errorMessages = getErrors(lifecycle),
        closeFormScreen = closeFormScreen
    )

}

