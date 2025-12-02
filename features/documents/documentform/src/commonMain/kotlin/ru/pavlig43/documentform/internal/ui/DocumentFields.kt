package ru.pavlig43.documentform.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.documentform.internal.data.DocumentEssentialsUi
import ru.pavlig43.manageitem.internal.ui.core_field_block.CommentFieldBlock
import ru.pavlig43.manageitem.internal.ui.core_field_block.ItemTypeField
import ru.pavlig43.manageitem.internal.ui.core_field_block.NameFieldBlock

@Composable
internal fun DocumentFields(
    document: DocumentEssentialsUi,
    updateDocument: (DocumentEssentialsUi) -> Unit,
) {
    NameFieldBlock(
        document.displayName,
        { updateDocument(document.copy(displayName = it)) }
    )

    ItemTypeField(
        typeVariants = DocumentType.entries,
        currentType = document.type,
        onChangeType = { updateDocument(document.copy(type = it)) }
    )

    CommentFieldBlock(
        comment = document.comment,
        onChangeComment = { updateDocument(document.copy(comment = it)) }
    )
}