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
import ru.pavlig43.files.api.ui.FilesScreen
import ru.pavlig43.core.ui.EssentialBlockScreen
import ru.pavlig43.product.api.component.ProductFormComponent
import ru.pavlig43.product.internal.component.tabs.tabslot.ProductEssentialsComponent
import ru.pavlig43.product.internal.component.tabs.tabslot.ProductTabChild
import ru.pavlig43.product.internal.ui.CompositionScreen
import ru.pavlig43.product.internal.ui.CreateProductScreen
import ru.pavlig43.product.internal.ui.DeclarationScreen
import ru.pavlig43.product.internal.ui.ProductFields
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
                is ProductFormComponent.Child.Create -> CreateProductScreen(instance.component)
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
            is ProductTabChild.Essentials -> UpdateEssentialsBlock(productTabChild.component)
            is ProductTabChild.Files -> FilesScreen(productTabChild.component)
            null -> Box(Modifier)
        }
    }

@Composable
private fun UpdateEssentialsBlock(
    productEssentialsComponent:ProductEssentialsComponent,
    modifier: Modifier = Modifier
){
    Column(modifier.verticalScroll(rememberScrollState())){
        EssentialBlockScreen(productEssentialsComponent) { item, onItemChange ->
            ProductFields(
                item,
                onItemChange
            )
        }
    }
}