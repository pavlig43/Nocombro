package ru.pavlig43.documentform.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.documentform.internal.toDocument
import ru.pavlig43.form.api.component.UpdateItemSlotComponent
import ru.pavlig43.form.api.data.IUpdateRepository

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
