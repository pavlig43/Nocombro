package ru.pavlig43.document.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.create.component.CreateEssentialsComponent
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.document.internal.data.DocumentEssentialsUi
import ru.pavlig43.document.internal.data.toDto

internal class CreateDocumentComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    createDocumentRepository: CreateEssentialsRepository<Document>,
    componentFactory: EssentialComponentFactory<Document, DocumentEssentialsUi>
) : CreateEssentialsComponent<Document, DocumentEssentialsUi>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    createEssentialsRepository = createDocumentRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() },

)
