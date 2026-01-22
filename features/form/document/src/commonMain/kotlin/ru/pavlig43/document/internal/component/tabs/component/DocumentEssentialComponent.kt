package ru.pavlig43.document.internal.component.tabs.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.document.internal.data.DocumentEssentialsUi
import ru.pavlig43.document.internal.data.toDto
import ru.pavlig43.update.component.UpdateEssentialsComponent
import ru.pavlig43.update.data.UpdateEssentialsRepository


internal class DocumentEssentialComponent(
    componentContext: ComponentContext,
    documentId: Int,
    updateRepository: UpdateEssentialsRepository<Document>,
    componentFactory: EssentialComponentFactory<Document, DocumentEssentialsUi>,
) : UpdateEssentialsComponent<Document, DocumentEssentialsUi>(
    componentContext = componentContext,
    id = documentId,
    updateEssentialsRepository = updateRepository,
    componentFactory = componentFactory,
    mapperToDTO = {toDto()}
){
    override val errorMessages: Flow<List<String>> = itemFields.map { doc->
        buildList {
            if (doc.displayName.isBlank()){
                add("Имя документа не может быть пустым")
            }
        }
    }

}

