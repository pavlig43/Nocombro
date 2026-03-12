package ru.pavlig43.vendor.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.files.api.ui.FilesScreen
import ru.pavlig43.update.ui.FormTabsUi
import ru.pavlig43.vendor.api.component.VendorFormComponent
import ru.pavlig43.vendor.internal.create.ui.CreateVendorSingleLineScreen
import ru.pavlig43.vendor.internal.update.VendorTabChild
import ru.pavlig43.vendor.internal.update.tabs.essential.UpdateVendorSingleLineScreen

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
                is VendorFormComponent.Child.Create -> CreateVendorSingleLineScreen(instance.component)
                is VendorFormComponent.Child.Update -> FormTabsUi(
                    component = instance.component,
                    tabChildFactory = { child: VendorTabChild? ->
                        VendorFormTabScreen(child)
                    }
                )
            }
        }

    }

}

@Composable
private fun VendorFormTabScreen(
    vendorChild: VendorTabChild?,
) {
    when (vendorChild) {
        is VendorTabChild.Essential -> UpdateVendorSingleLineScreen(vendorChild.component)
        is VendorTabChild.Files -> FilesScreen(vendorChild.component)
        null -> Box(Modifier.fillMaxSize()) { Text("Пусто") }
    }
}
