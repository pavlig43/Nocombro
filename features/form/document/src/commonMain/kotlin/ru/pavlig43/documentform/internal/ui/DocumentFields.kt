package ru.pavlig43.documentform.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.coreui.coreFieldBlock.CommentFieldBlock
import ru.pavlig43.coreui.coreFieldBlock.ItemTypeField
import ru.pavlig43.coreui.coreFieldBlock.NameFieldBlock
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.documentform.internal.data.DocumentEssentialsUi

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