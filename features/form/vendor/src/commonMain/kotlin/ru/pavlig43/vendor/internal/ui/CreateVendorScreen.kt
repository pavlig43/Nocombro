package ru.pavlig43.vendor.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.create.ui.CreateEssentialsScreen
import ru.pavlig43.vendor.internal.component.CreateVendorComponent


@Composable
internal fun CreateVendorScreen(
    component: CreateVendorComponent
) {

    CreateEssentialsScreen(component) { item, onItemChange ->
        VendorFields(
            vendor = item,
            updateVendor = onItemChange,
        )
    }
}