package ru.pavlig43.document.api.component


import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.serialization.Serializable
import org.koin.core.scope.Scope
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.document.api.IDocumentDependencies
import ru.pavlig43.document.api.data.DocumentUi
import ru.pavlig43.document.internal.di.createModule
import ru.pavlig43.itemlist.api.component.ItemListComponent

class DocumentComponent(
    componentContext: ComponentContext,
    dependencies: IDocumentDependencies
) : ComponentContext by componentContext, IDocumentComponent {
    private val coroutineScope = componentCoroutineScope()
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createModule(dependencies))

    private val stackNavigation = StackNavigation<Config>()
    override val stack: Value<ChildStack<*, IDocumentComponent.Child>> = childStack(
        source = stackNavigation,
        serializer = Config.serializer(),
        handleBackButton = true,
        initialConfiguration = Config.ItemList,
        childFactory = ::createChild
    )

    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): IDocumentComponent.Child {
        return when (config) {
            is Config.ItemList -> IDocumentComponent.Child.ItemList(
                ItemListComponent<Document, DocumentUi, DocumentType>(
                    componentContext = componentContext,
                    repository = scope.get(),
                    onCreateScreen = {
                        stackNavigation.pushToFront(
                            Config.CreateDocument
                        )
                    }
                )
            )

            Config.CreateDocument -> IDocumentComponent.Child.CreateDocument(
                CreateDocumentComponent(
                    componentContext = componentContext,
                    repository = scope.get(),
                    onBackScreen = {
                        stackNavigation.pop()
                    }
                )
            )
        }
    }


    @Serializable
    sealed interface Config {
        @Serializable
        data object ItemList : Config
        @Serializable
        data object CreateDocument : Config
    }

}

