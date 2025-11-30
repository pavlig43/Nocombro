package ru.pavlig43.documentform.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.documentform.internal.data.DocumentEssentialsUi
import ru.pavlig43.documentform.internal.toDocument
import ru.pavlig43.form.api.component.UpdateItemSlotComponent
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.manageitem.api.data.CreateEssentialsRepository
import ru.pavlig43.manageitem.internal.component.EssentialsComponent
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class DocumentRequiresTabSlot(
    componentContext: ComponentContext,
    documentId: Int,
    updateRepository: IUpdateRepository<Document, Document>,
    onChangeValueForMainTab: (String) -> Unit
) : UpdateItemSlotComponent<Document, DocumentType>(
    componentContext = componentContext,
    id = documentId,
    typeVariantList = DocumentType.entries,
    updateRepository = updateRepository,
    mapper = { toDocument() },
    onChangeValueForMainTab = onChangeValueForMainTab
), DocumentTabSlot {

    override val title: String = "Основная информация"
}

@OptIn(ExperimentalTime::class)
internal class DocumentComponent(
    componentContext: ComponentContext,
    documentId: Int,
    onChangeValueForMainTab: (String) -> Unit,
    onSuccessUpsert:(Int)-> Unit,
    createEssentialsRepository: CreateEssentialsRepository<Document>,
) : EssentialsComponent<Document, DocumentEssentialsUi>(
    componentContext = componentContext,
    initItem = DocumentEssentialsUi(),
    isValidValuesFactory = { displayName.isNotBlank() && type != null },
    mapperToDTO = {this.toDto()},
    mapperToUi = {this.toUi()},
    onSuccessUpsert = onSuccessUpsert,
    vendorInfoForTabName = { document -> onChangeValueForMainTab("*(Документ) ${document.displayName}") },
    upsertEssentialsRepository = createEssentialsRepository,
), DocumentTabSlot {
    override val title: String = "Основная информация"

    override suspend fun onUpdate() {
        upsert()
    }
}

@OptIn(ExperimentalTime::class)
private fun DocumentEssentialsUi.toDto(): Document {
    return Document(
        displayName = displayName,
        type = type ?: throw IllegalArgumentException("Document type required"),
        createdAt = createdAt ?: Clock.System.now().toEpochMilliseconds(),
        comment = comment,
        id = id
    )
}
private fun Document.toUi(): DocumentEssentialsUi {
    return DocumentEssentialsUi(
        displayName = displayName,
        type = type ,
        createdAt = createdAt ,
        comment = comment,
        id = id
    )
}
