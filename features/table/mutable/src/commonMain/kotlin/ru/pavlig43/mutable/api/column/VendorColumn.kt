package ru.pavlig43.mutable.api.column

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType

@Suppress("LongParameterList")
fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.vendorNameColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> String?,
    onOpenDialog: () -> Unit,
    filterType: TableFilterType<*>? = null,
    isSortable: Boolean = true,
    alignment: Alignment = Alignment.Center
) {
    column(column, valueOf = { it }) {
        autoWidth(300.dp)
        header(headerText)
        align(alignment)
        filterType?.let {
            filter(it)
        }
        cell { item, _ ->
            VendorNameRow(
                vendorName = valueOf(item),
                onOpenDialog = onOpenDialog
            )
        }
        if (isSortable) {
            sortable()
        }
    }
}

@Composable
private fun VendorNameRow(
    vendorName: String?,
    onOpenDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Поставщик")
        if (vendorName != null) {
            Text(vendorName, textDecoration = TextDecoration.Underline)
        }
        ToolTipIconButton(
            tooltipText = if (vendorName == null) "Добавить поставщика" else "Изменить поставщика",
            onClick = onOpenDialog,
            icon = if (vendorName == null) Res.drawable.add_circle else Res.drawable.refresh
        )
    }
}
