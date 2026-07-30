package ru.pavlig43.vendor.internal.update.tabs.essential

import androidx.compose.runtime.Composable
import ru.pavlig43.vendor.internal.VendorEssentialsCards

/** Показывает карточную форму правки поставщика. */
@Composable
internal fun UpdateVendorSingleLineScreen(
    component: VendorUpdateSingleLineComponent,
) {
    VendorEssentialsCards(component)
}
