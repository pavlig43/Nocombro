package ru.pavlig43.manageitem.internal.ui.core_field_block

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.tooltip.IconButtonToolTip


@Composable
internal fun VendorFieldBlock(
    vendorName: String?,
    onOpenVendorDialog: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Поставщик")
        if (vendorName != null) {
            Text(vendorName, textDecoration = TextDecoration.Underline)
        }
        IconButtonToolTip(
            tooltipText = if (vendorName == null) "Добавить поставщика" else "Изменить поставщика",
            onClick = onOpenVendorDialog,
            icon = if (vendorName == null) Icons.Default.AddCircle else Icons.Default.Refresh,
        )
    }

}