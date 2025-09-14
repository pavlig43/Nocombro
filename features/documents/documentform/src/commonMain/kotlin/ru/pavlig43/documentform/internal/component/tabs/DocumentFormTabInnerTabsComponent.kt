package ru.pavlig43.documentform.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.tabs.DefaultTabNavigationComponent
import ru.pavlig43.core.tabs.ITabNavigationComponent
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.documentform.internal.di.UpdateRepositoryType
import ru.pavlig43.form.api.component.IItemFormInnerTabsComponent
import ru.pavlig43.upsertitem.api.component.IUpdateComponent
import ru.pavlig43.upsertitem.api.component.UpdateComponent

internal class DocumentFormTabInnerTabsComponent(
    componentContext: ComponentContext,
    closeFormScreen:()->Unit,
    scope: Scope,
    documentId: Int,
    onChangeValueForMainTab: (String) -> Unit
) : ComponentContext by componentContext, IItemFormInnerTabsComponent<DocumentTab, DocumentTabSlot> {

    private val _mainTabTitle = MutableStateFlow("")
    override val mainTabTitle: StateFlow<String> = _mainTabTitle.asStateFlow()
    private val dbTransaction:DataBaseTransaction = scope.get()


    override val tabNavigationComponent: ITabNavigationComponent<DocumentTab, DocumentTabSlot> =
        DefaultTabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                DocumentTab.RequireValues,
                DocumentTab.Files
            ),
            serializer = DocumentTab.serializer(),
            slotFactory = { context, tabConfig: DocumentTab, _: (DocumentTab) -> Unit, _: () -> Unit ->
                when (tabConfig) {

                    DocumentTab.RequireValues ->DocumentRequiresTabSlot(
                        componentContext = context,
                        documentId = documentId,
                        updateRepository = scope.get(named(UpdateRepositoryType.Document.name)),
                        onChangeValueForMainTab = onChangeValueForMainTab
                    )


                    DocumentTab.Files -> DocumentFileTabSlot(
                        documentId = documentId,
                        updateRepository = scope.get(),
                        componentContext = context
                    )
                }

            },
        )
    private suspend fun update():RequestResult<Unit> {
        val blocks: Value<List<suspend () -> Unit>> = tabNavigationComponent.children.map { children->
            children.items.map { child-> suspend {child.instance.onUpdate()} } }
        println("blocks $blocks")
        println("blocks ${blocks.value}")
        println(tabNavigationComponent.children.map { it.items.map { it.instance.title } })
        return dbSafeCall("document form"){
            dbTransaction.transaction(blocks.value)
        }
    }
    override val updateComponent: IUpdateComponent = UpdateComponent(
        componentContext = childContext("update"),
        onUpdateComponent = {update()},
        closeFormScreen = closeFormScreen
    )



}

