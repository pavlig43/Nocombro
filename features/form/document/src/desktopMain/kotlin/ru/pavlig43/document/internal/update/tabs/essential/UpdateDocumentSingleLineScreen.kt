package ru.pavlig43.document.internal.update.tabs.essential

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.datetime.single.date.DatePickerDialog
import ru.pavlig43.document.internal.DocumentEssentialsCards

/** Показывает карточную форму правки документа. */
@Composable
internal fun UpdateDocumentSingleLineScreen(
    component: DocumentUpdateSingleLineComponent,
) {
    val dialog by component.dialog.subscribeAsState()
    DocumentEssentialsCards(
        component = component,
        onOpenDateDialog = component::onOpenDateDialog,
    )
    dialog.child?.instance?.also {
        DatePickerDialog(it)
    }
}