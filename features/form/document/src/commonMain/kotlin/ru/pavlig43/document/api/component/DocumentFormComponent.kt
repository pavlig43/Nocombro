package ru.pavlig43.document.api.component

import com.arkivanov.decompose.ComponentContext
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
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.document.api.DocumentFormDependencies
import ru.pavlig43.document.internal.component.CreateDocumentSingleLineComponent
import ru.pavlig43.document.internal.component.tabs.DocumentFormTabsComponent
import ru.pavlig43.document.internal.data.DocumentEssentialsUi
import ru.pavlig43.document.internal.data.toUi
import ru.pavlig43.document.internal.di.createDocumentFormModule


class DocumentFormComponent(
    documentId: Int,
    val closeTab: () -> Unit,
    componentContext: ComponentContext,
    dependencies: DocumentFormDependencies,
) : ComponentContext by componentContext, MainTabComponent {

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createDocumentFormModule(dependencies))


    private val _model = MutableStateFlow(MainTabComponent.NavTabState(""))
    override val model = _model.asStateFlow()

    private val stackNavigation = StackNavigation<Config>()


    private val essentialFactory = EssentialComponentFactory<Document, DocumentEssentialsUi>(
        initItem = DocumentEssentialsUi(),
        isValidFieldsFactory = { displayName.isNotBlank() && type != null },
        mapperToUi = { toUi() },
        produceInfoForTabName = { d -> onChangeValueForMainTab("*Документ ${d.displayName}") }
    )

    private fun onChangeValueForMainTab(title: String) {

        val navTabState = MainTabComponent.NavTabState(title)
        _model.update { navTabState }
    }
    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Child {
        return when (config) {
            is Config.Create -> Child.Create(
                ru.pavlig43.document.internal.component.CreateDocumentSingleLineComponent(
                    componentContext = componentContext,
                    onSuccessCreate = { stackNavigation.replaceAll(Config.Update(it)) },
                    createDocumentRepository = scope.get()
                )

            )

            is Config.Update -> Child.Update(
                DocumentFormTabsComponent(
                    componentContext = componentContext,
                    essentialFactory = essentialFactory,
                    scope = scope,
                    documentId = config.id,
                    closeFormScreen = closeTab
                )
            )
        }
    }
    internal val stack: Value<ChildStack<Config, Child>> = childStack(
        source = stackNavigation,
        serializer = Config.serializer(),
        initialConfiguration = if (documentId == 0) Config.Create else Config.Update(documentId),
        handleBackButton = false,
        childFactory = ::createChild
    )

    @Serializable
    sealed interface Config {
        @Serializable
        data object Create : Config

        @Serializable
        data class Update(val id: Int) : Config
    }

    internal sealed class Child {
        class Create(val component: ru.pavlig43.document.internal.component.CreateDocumentSingleLineComponent) : Child()
        class Update(val component: DocumentFormTabsComponent) : Child()
    }
}





