package ru.pavlig43.product.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import ru.pavlig43.core.ui.EssentialBlockScreen
import ru.pavlig43.mutable.api.ui.MutableTableBox
import ru.pavlig43.product.api.component.ProductFormComponent
import ru.pavlig43.product.internal.component.tabs.tabslot.CompositionTabSlot
import ru.pavlig43.product.internal.component.tabs.tabslot.CompositionTabSlot1
import ru.pavlig43.product.internal.component.tabs.tabslot.DeclarationTabSlot1
import ru.pavlig43.product.internal.component.tabs.tabslot.EssentialTabSlot
import ru.pavlig43.product.internal.component.tabs.tabslot.ProductFileTabSlot
import ru.pavlig43.product.internal.component.tabs.tabslot.ProductTabSlot
import ru.pavlig43.product.internal.ui.CompositionScreen
import ru.pavlig43.product.internal.ui.CreateProductScreen
import ru.pavlig43.product.internal.ui.DeclarationScreen
import ru.pavlig43.product.internal.ui.ProductFields
import ru.pavlig43.update.ui.ItemTabsUi

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
            is EssentialTabSlot -> UpdateEssentialsBlock(productSlot)

            is ProductFileTabSlot -> FilesScreen(productSlot.fileComponent)
            is DeclarationTabSlot1 -> DeclarationScreen(productSlot)


            is CompositionTabSlot -> CompositionScreen(productSlot)


            null -> Box(Modifier)
            is CompositionTabSlot1 -> MutableTableBox(productSlot)
        }
    }

@Composable
private fun UpdateEssentialsBlock(
    essentialTabSlot:EssentialTabSlot,
    modifier: Modifier = Modifier
){
    Column(modifier.verticalScroll(rememberScrollState())){
        EssentialBlockScreen(essentialTabSlot) { item, onItemChange ->
            ProductFields(
                item,
                onItemChange
            )
        }
    }
}