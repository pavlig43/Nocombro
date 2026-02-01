package ru.pavlig43.coreui.coreFieldBlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.add_circle
import ru.pavlig43.theme.refresh


@Composable
fun VendorFieldBlock(
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
        ToolTipIconButton(
            tooltipText = if (vendorName == null) "Добавить поставщика" else "Изменить поставщика",
            onClick = onOpenVendorDialog,
            icon = if (vendorName == null) Res.drawable.add_circle else Res.drawable.refresh,
        )
    }

}