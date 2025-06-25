package ru.pavlig43.document.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.scope.Scope
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.createitem.api.component.CreateItemComponent
import ru.pavlig43.createitem.api.component.ICreateItemComponent
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.document.api.IDocumentDependencies
import ru.pavlig43.document.internal.di.createModule

class CreateDocumentComponent(
    componentContext: ComponentContext,
    dependencies: IDocumentDependencies,
//    private val repository: ICreateItemRepository<DocumentType>
) : ComponentContext by componentContext, ICreateDocumentComponent,SlotComponent {

//    private val coroutineScope = componentCoroutineScope()
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope = koinContext.getOrCreateKoinScope(createModule(dependencies))

    override val createBaseRowsOfComponent: ICreateItemComponent =
        CreateItemComponent<DocumentType>(
            componentContext = childContext("createBaseRowsOfComponent"),
            repository = scope.get(),
        )



    private val _model = MutableStateFlow(SlotComponent.TabModel(TAB_TITLE))
    override val model: StateFlow<SlotComponent.TabModel> = _model.asStateFlow()

    private companion object {
        const val TAB_TITLE = "* Создание документа"
    }

}