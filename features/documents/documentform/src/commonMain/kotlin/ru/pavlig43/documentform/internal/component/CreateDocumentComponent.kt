package ru.pavlig43.documentform.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.documentform.internal.data.DocumentEssentialsUi
import ru.pavlig43.documentform.internal.data.toDto
import ru.pavlig43.manageitem.api.data.CreateEssentialsRepository
import ru.pavlig43.manageitem.internal.component.CreateEssentialsComponent
import ru.pavlig43.manageitem.internal.component.EssentialComponentFactory

internal class CreateDocumentComponent(
    componentContext: ComponentContext,
    onSuccessCreate:(Int)-> Unit,
    createDocumentRepository: CreateEssentialsRepository<Document>,
    componentFactory: EssentialComponentFactory<Document, DocumentEssentialsUi>
): CreateEssentialsComponent<Document, DocumentEssentialsUi>(
    componentContext = componentContext,
    onSuccessCreate =onSuccessCreate,
    createEssentialsRepository = createDocumentRepository,
    componentFactory = componentFactory,
    mapperToDTO = {toDto()},
)