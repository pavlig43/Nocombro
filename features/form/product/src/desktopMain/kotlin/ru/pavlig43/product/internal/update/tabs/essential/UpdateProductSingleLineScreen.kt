package ru.pavlig43.product.internal.update.tabs.essential

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.datetime.single.date.DatePickerDialog
import ru.pavlig43.product.internal.ProductEssentialsCards

@Composable
internal fun UpdateProductSingleLineScreen(
    component: ProductUpdateSingleLineComponent
) {
    val dialog by component.dialog.subscribeAsState()
    ProductEssentialsCards(
        component = component,
        onOpenDateDialog = component::onOpenDateDialog,
    )
    dialog.child?.instance?.also {
        DatePickerDialog(it)
    }
}
