package ru.pavlig43.itemlist.internal.ui.core

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectItemCheckBox(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkboxWidth: Int,
    modifier: Modifier = Modifier
) {
    Checkbox(
        checked = isChecked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.width(checkboxWidth.dp)
    )
}