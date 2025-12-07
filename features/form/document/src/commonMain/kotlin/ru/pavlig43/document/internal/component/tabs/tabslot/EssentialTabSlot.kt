package ru.pavlig43.document.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.document.internal.data.DocumentEssentialsUi
import ru.pavlig43.document.internal.data.toDto
import ru.pavlig43.update.component.UpdateEssentialsComponent
import ru.pavlig43.update.data.UpdateEssentialsRepository


internal class EssentialTabSlot(
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
), DocumentTabSlot

