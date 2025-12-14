package ru.pavlig43.itemlist.internal.ui.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.FilterAlt
import androidx.compose.material.icons.sharp.FilterAltOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.itemlist.core.component.ValueFilterComponent


@Composable
internal fun <I : ItemType> LabelSelectionLogic(
    label: String,
    component: ValueFilterComponent<List<I>>,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        SelectionLogic(component)
    }
}

@Composable
internal fun <T : ItemType> SelectionLogic(
    component: ValueFilterComponent<List<T>>,
) {

    var isMenuOpen by remember { mutableStateOf(false) }

    val selectedItems = remember { mutableStateListOf<T>() }

    IconButtonToolTip(
        tooltipText = "Отбор",
        onClick = { isMenuOpen = !isMenuOpen },
        icon = if (selectedItems.isEmpty()) Icons.Sharp.FilterAltOff else Icons.Sharp.FilterAlt,
        badge = { if (selectedItems.isNotEmpty()) Badge() }
    )
    DropdownMenu(
        expanded = isMenuOpen,
        onDismissRequest = { isMenuOpen = false }
    ) {
        DropDownBody(
            items = component.initialValue,
            selectedItems = selectedItems,
            addItemInSelection = { selectedItems.add(it) },
            removeItemFromSelection = { selectedItems.remove(it) },
            saveSelection = {
                isMenuOpen = false
                component.onChange(selectedItems.toList())
            }
        )

    }
}

@Suppress("LongParameterList")
@Composable
private fun <T : ItemType> DropDownBody(
    items: List<T>,
    selectedItems: List<T>,
    addItemInSelection: (T) -> Unit,
    removeItemFromSelection: (T) -> Unit,
    saveSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items.forEach { item ->
            CheckboxItem(
                item = item,
                isChecked = item in selectedItems,
                onCheckedChange = { isChecked ->
                    if (isChecked) addItemInSelection(item) else removeItemFromSelection(item)
                }
            )
        }
        Button(saveSelection) {
            Text("Сохранить обор")
        }

    }
}

@Composable
private fun CheckboxItem(
    item: ItemType,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier
            .fillMaxWidth().clickable { onCheckedChange(!isChecked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
        Text(item.displayName)
    }
}

