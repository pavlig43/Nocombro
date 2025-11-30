package ru.pavlig43.vendor.api.ui

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
import ru.pavlig43.form.api.ui.ItemTabsUi
import ru.pavlig43.manageitem.api.ui.CreateItemScreen
import ru.pavlig43.vendor.api.VendorFormComponent
import ru.pavlig43.vendor.internal.component.VendorFileTabSlot
import ru.pavlig43.vendor.internal.component.VendorInformationTabSlot
import ru.pavlig43.vendor.internal.component.VendorTabSlot

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
                is VendorFormComponent.Child.Create -> CreateItemScreen(instance.component)
                is VendorFormComponent.Child.Update -> ItemTabsUi(
                    component = instance.component,
                    slotFactory = { slotForm: VendorTabSlot? ->
                        VendorSlotScreen(slotForm)
                    })
            }
        }

    }

}

@Composable
private fun VendorSlotScreen(vendorSlot: VendorTabSlot?) {
    when (vendorSlot) {
//        is VendorRequiresTabSlot -> RequireValuesScreen(vendorSlot.requires)


        is VendorFileTabSlot -> FilesScreen(vendorSlot.fileComponent)

        is VendorInformationTabSlot -> Box(Modifier)

        else -> error("vendor slot not found $vendorSlot")
    }
}