package ru.pavlig43.product.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.files.api.ui.FilesScreen
import ru.pavlig43.product.api.component.ProductFormComponent
import ru.pavlig43.product.internal.create.ui.CreateProductSingleLineScreen
import ru.pavlig43.product.internal.update.ProductTabChild
import ru.pavlig43.product.internal.update.tabs.composition.CompositionScreen
import ru.pavlig43.product.internal.update.tabs.DeclarationScreen
import ru.pavlig43.product.internal.update.tabs.essential.UpdateProductSingleLineScreen
import ru.pavlig43.update.ui.FormTabsUi

@Composable
fun ProductFormScreen(
    component: ProductFormComponent,
    modifier: Modifier = Modifier,
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier = modifier
            .padding(horizontal = 8.dp)
    ) {
        val stack by component.stack.subscribeAsState()
        Children(
            stack = stack,
        ) { child ->
            when (val instance = child.instance) {
                is ProductFormComponent.Child.Create -> CreateProductSingleLineScreen(instance.component)
                is ProductFormComponent.Child.Update -> FormTabsUi(
                    component = instance.component,
                    tabChildFactory = { slotForm: ProductTabChild? ->
                        ProductSlotScreen(slotForm)
                    })
            }
        }

    }

}

@Composable
private fun ProductSlotScreen(productTabChild: ProductTabChild?) {
    when (productTabChild) {
        is ProductTabChild.Composition -> CompositionScreen(productTabChild.component)
        is ProductTabChild.Declaration -> DeclarationScreen(productTabChild.component)
        is ProductTabChild.Essentials -> UpdateProductSingleLineScreen(productTabChild.component)
        is ProductTabChild.Files -> FilesScreen(productTabChild.component)
        null -> Box(Modifier)
    }
}
