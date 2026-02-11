package ru.pavlig43.product.internal.create.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.DatePickerDialog
import ru.pavlig43.product.internal.create.component.CreateProductSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.ui.CreateSingleItemScreen

/**
 * UI экран для создания продукта через таблицу с одной строкой
 *
 * @param component Компонент создания продукта
 */
@Composable
internal fun CreateProductSingleLineScreen(
    component: CreateProductSingleLineComponent
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
