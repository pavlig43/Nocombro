package ru.pavlig43.documentform.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import org.koin.core.scope.Scope
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.documentform.api.DocumentFormDependencies
import ru.pavlig43.documentform.internal.component.tabs.DocumentFormTabInnerTabsComponent
import ru.pavlig43.documentform.internal.di.createDocumentFormModule
import ru.pavlig43.documentform.internal.toDocument
import ru.pavlig43.manageitem.api.component.CreateItemComponent
import ru.pavlig43.manageitem.api.data.DefaultRequireValues


class DocumentFormComponent(
    documentId: Int,
    val closeTab: () -> Unit,
    componentContext: ComponentContext,
    dependencies: DocumentFormDependencies,
) : ComponentContext by componentContext, SlotComponent {

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createDocumentFormModule(dependencies))


    private val _model = MutableStateFlow(SlotComponent.TabModel(""))
    override val model = _model.asStateFlow()

    private val stackNavigation = StackNavigation<Config>()

    internal val stack: Value<ChildStack<Config, Child>> = childStack(
        source = stackNavigation,
        serializer = Config.serializer(),
        initialConfiguration = if (documentId == 0) Config.Create else Config.Update(documentId),
        handleBackButton = false,
        childFactory = ::createChild
    )


    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Child {
        return when (config) {
            is Config.Create -> Child.Create(
                CreateItemComponent(
                    componentContext = componentContext,
                    typeVariantList = DocumentType.entries,
                    mapper = DefaultRequireValues::toDocument,
                    createItemRepository = scope.get(),
                    onSuccessCreate = {stackNavigation.replaceAll(Config.Update(it))},
                    onChangeValueForMainTab = {onChangeValueForMainTab("* $it")}
                )
            )

            is Config.Update -> Child.Update(
                DocumentFormTabInnerTabsComponent(
                    componentContext = childContext("document_form"),
                    scope = scope,
                    documentId = config.id,
                    closeFormScreen = closeTab,
                    onChangeValueForMainTab = {onChangeValueForMainTab(it)}
                )
            )
        }
    }


    private fun onChangeValueForMainTab(title: String) {

        val tabModel = SlotComponent.TabModel(title)
        _model.update { tabModel }
    }




    @Serializable
    sealed interface Config {
        @Serializable
        data object Create : Config

        @Serializable
        data class Update(val id: Int) : Config
    }

    internal sealed class Child {
        class Create(val component: CreateItemComponent<Document,DocumentType>) : Child()
        class Update(val component: DocumentFormTabInnerTabsComponent) : Child()
    }
}





