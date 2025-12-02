package ru.pavlig43.productform.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.manageitem.internal.ui.CreateEssentialsScreen
import ru.pavlig43.productform.internal.component.CreateProductComponent


@Composable
internal fun CreateProductScreen(
    component: CreateProductComponent
) {

    CreateEssentialsScreen(component) { item, onItemChange ->
        ProductFields(
            product = item,
            updateProduct = onItemChange,
        )
    }
}