package ru.pavlig43.document.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import org.koin.core.scope.Scope
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.createitem.api.component.CreateItemComponent
import ru.pavlig43.createitem.api.component.ICreateItemComponent
import ru.pavlig43.createitem.api.data.ICreateItemRepository
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.document.api.IDocumentDependencies
import ru.pavlig43.document.internal.di.createModule

class CreateDocumentComponent(
    componentContext: ComponentContext,
    private val onBackScreen: () -> Unit,
    private val repository: ICreateItemRepository<DocumentType>
) : ComponentContext by componentContext, ICreateDocumentComponent {

    private val coroutineScope = componentCoroutineScope()
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }

    override val createBaseRowsOfComponent: ICreateItemComponent =
        CreateItemComponent<DocumentType>(
            componentContext = childContext("createBaseRowsOfComponent"),
            repository = repository
        )

    override fun onBack() {
        onBackScreen()
    }
}