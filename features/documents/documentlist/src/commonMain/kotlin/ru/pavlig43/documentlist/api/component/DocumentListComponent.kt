package ru.pavlig43.documentlist.api.component


import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.scope.Scope
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.documentlist.api.IDocumentLisDependencies
import ru.pavlig43.documentlist.api.data.DocumentUi
import ru.pavlig43.documentlist.internal.di.createModule
import ru.pavlig43.itemlist.api.component.IItemListComponent
import ru.pavlig43.itemlist.api.component.ItemListComponent

class DocumentListComponent(
    componentContext: ComponentContext,
    onItemClick:(Int)-> Unit,
    onCreateScreen: () -> Unit,
    dependencies: IDocumentLisDependencies
) : ComponentContext by componentContext, IDocumentListComponent, SlotComponent {
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createModule(dependencies))


    private val _model = MutableStateFlow(SlotComponent.TabModel(TAB_TITLE))
    override val model: StateFlow<SlotComponent.TabModel> = _model.asStateFlow()

    private companion object {
        const val TAB_TITLE = "Документы"
    }
    override val itemListComponent: IItemListComponent =
        ItemListComponent<Document, DocumentUi, DocumentType>(
            componentContext = componentContext,
            fullListSelection = DocumentType.entries,
            repository = scope.get(),
            onCreateScreen = onCreateScreen,
            onItemClick = onItemClick
        )

}
