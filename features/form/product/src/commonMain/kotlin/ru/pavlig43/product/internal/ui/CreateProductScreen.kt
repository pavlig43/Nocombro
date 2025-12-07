package ru.pavlig43.product.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.create.ui.CreateEssentialsScreen
import ru.pavlig43.product.internal.component.CreateProductComponent


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