package ru.pavlig43.manageitem.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.manageitem.api.DocumentFactoryParam
import ru.pavlig43.manageitem.api.data.CreateEssentialsRepository
import ru.pavlig43.manageitem.internal.data.DocumentEssentialsUi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class DocumentComponent(
    componentContext: ComponentContext,
    param: DocumentFactoryParam,
    createEssentialsRepository: CreateEssentialsRepository<Document>,
) : EssentialsComponent<Document, DocumentEssentialsUi>(
    componentContext = componentContext,
    initItem = DocumentEssentialsUi(),
    isValidValuesFactory = { displayName.isNotBlank() && type != null },
    mapperToDTO = {
        Document(
            displayName = displayName,
            type = type ?: throw IllegalArgumentException("Document type required"),
            createdAt = createdAt ?: Clock.System.now().toEpochMilliseconds(),
            comment = comment,
            id = 0
        )
    },
    onSuccessUpsert = param.onSuccessUpsert,
    vendorInfoForTabName = { document -> param.onChangeValueForMainTab("*(Документ) ${document.displayName}") },
    upsertEssentialsRepository = createEssentialsRepository,
)
