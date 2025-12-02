package ru.pavlig43.productform.api.ui

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
import ru.pavlig43.addfile.api.ui.FilesScreen
import ru.pavlig43.declaration.api.ui.ProductDeclarationScreen
import ru.pavlig43.update.ui.ItemTabsUi
import ru.pavlig43.core.ui.EssentialBlockScreen
import ru.pavlig43.productform.api.component.ProductFormComponent
import ru.pavlig43.productform.internal.component.tabs.tabslot.*
import ru.pavlig43.productform.internal.ui.CompositionScreen
import ru.pavlig43.productform.internal.ui.CreateProductScreen
import ru.pavlig43.productform.internal.ui.ProductFields

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
                is ProductFormComponent.Child.Create -> CreateProductScreen(instance.component)
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
        is EssentialTabSlot -> EssentialBlockScreen(productSlot) { item, updateItem ->
            ProductFields(item, updateItem)
        }

        is ProductFileTabSlot -> FilesScreen(productSlot.fileComponent)

        is ProductDeclarationTabSlot -> ProductDeclarationScreen(productSlot)

        is CompositionTabSlot -> CompositionScreen(productSlot)


        null -> Box(Modifier)
    }
}