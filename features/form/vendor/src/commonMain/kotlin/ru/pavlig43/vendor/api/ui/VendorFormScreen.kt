package ru.pavlig43.vendor.api.ui

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
import ru.pavlig43.update.ui.ItemTabsUi1
import ru.pavlig43.vendor.component.VendorFormComponent
import ru.pavlig43.vendor.internal.component.tabs.tabslot.VendorEssentialsComponent
import ru.pavlig43.vendor.internal.component.tabs.tabslot.VendorTabChild
import ru.pavlig43.vendor.internal.ui.CreateVendorScreen
import ru.pavlig43.vendor.internal.ui.VendorFields

@Composable
fun VendorFormScreen(
    component: VendorFormComponent,
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
                is VendorFormComponent.Child.Create -> CreateVendorScreen(instance.component)
                is VendorFormComponent.Child.Update -> ItemTabsUi1(
                    component = instance.component,
                    slotFactory = { child ->
                        VendorTabsScreen(child)
                    })
            }
        }

    }

}

@Composable
private fun VendorTabsScreen(child: VendorTabChild?) {
        when (child) {
            null -> Box(Modifier)
            is VendorTabChild.Essentials -> UpdateEssentialsBlock(child.component)
            is VendorTabChild.Files -> FilesScreen(child.component)
        }
}
@Composable
private fun UpdateEssentialsBlock(
    essentials: VendorEssentialsComponent,
    modifier: Modifier = Modifier
){
    Column(modifier.verticalScroll(rememberScrollState())){
        EssentialBlockScreen(essentials) { item, onItemChange ->
            VendorFields(
                item,
                onItemChange
            )
        }
    }
}