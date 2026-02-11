package ru.pavlig43.document.internal.create.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.DatePickerDialog
import ru.pavlig43.document.internal.create.component.CreateDocumentSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.ui.CreateSingleItemScreen

/**
 * UI экран для создания документа через таблицу с одной строкой
 *
 * @param component Компонент создания документа
 */
@Composable
internal fun CreateDocumentSingleLineScreen(
    component: CreateDocumentSingleLineComponent
) {
    val dialog by component.dialog.subscribeAsState()


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        CreateSingleItemScreen(component)
    }

    // Отображение диалога выбора даты
    dialog.child?.instance?.also { dateComponent ->
        DatePickerDialog(dateComponent)
    }
}

