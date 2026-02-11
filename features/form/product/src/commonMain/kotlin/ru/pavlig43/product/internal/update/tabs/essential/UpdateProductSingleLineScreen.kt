package ru.pavlig43.product.internal.update.tabs.essential

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.DatePickerDialog
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineBlockScreen

@Composable
internal fun UpdateProductSingleLineScreen(
    component: ProductUpdateSingleLineComponent
) {
    val dialog by component.dialog.subscribeAsState()
    SingleLineBlockScreen(component)
    dialog.child?.instance?.also {
        DatePickerDialog(it)
    }
}
