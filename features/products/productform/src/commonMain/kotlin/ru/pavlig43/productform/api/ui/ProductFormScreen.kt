package ru.pavlig43.productform.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.addfile.api.ui.FilesScreen
import ru.pavlig43.declaration.api.ui.ProductDeclarationScreen
import ru.pavlig43.form.api.ui.ItemTabsUi
import ru.pavlig43.manageitem.api.ui.CreateScreen
import ru.pavlig43.manageitem.api.ui.RequireValuesScreen
import ru.pavlig43.productform.api.component.ProductFormComponent
import ru.pavlig43.productform.internal.component.CompositionTabSlot
import ru.pavlig43.productform.internal.component.ProductDeclarationTabSlot
import ru.pavlig43.productform.internal.component.ProductFileTabSlot
import ru.pavlig43.productform.internal.component.ProductRequiresTabSlot
import ru.pavlig43.productform.internal.component.ProductTabSlot
import ru.pavlig43.productform.internal.ui.CompositionScreen

@Composable
fun ProductFormScreen(
    component: ProductFormComponent,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier = modifier
            .padding(horizontal = 8.dp)
            .verticalScroll(scrollState)
    ) {
        val stack by component.stack.subscribeAsState()
        Children(
            stack = stack,
        ) { child ->
            when (val instance = child.instance) {
                is ProductFormComponent.Child.Create -> CreateScreen(instance.component)
                is ProductFormComponent.Child.Update -> ItemTabsUi(
                    component = instance.component,
                    slotFactory = { slotForm: ProductTabSlot? ->
                        ProductSlotScreen(slotForm)
                    })
            }
        }

    }

}

@Composable
private fun ProductSlotScreen(productSlot: ProductTabSlot?) {
    when (productSlot) {
        is ProductRequiresTabSlot -> RequireValuesScreen(productSlot.requires)

        is ProductFileTabSlot -> FilesScreen(productSlot.fileComponent)

        is ProductDeclarationTabSlot -> ProductDeclarationScreen(productSlot)

        is CompositionTabSlot -> CompositionScreen(productSlot)

        else -> error("product slot not found $productSlot")
    }
}