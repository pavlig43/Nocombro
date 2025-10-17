package ru.pavlig43.coreui.itemlist

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun SelectItemCheckBox(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkboxWidth: Dp,
    modifier: Modifier = Modifier
) {
    Checkbox(
        checked = isChecked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.width(checkboxWidth)
    )
}