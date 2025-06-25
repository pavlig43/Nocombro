package ru.pavlig43.itemlist.internal.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.pavlig43.database.data.common.data.ItemType
import ru.pavlig43.itemlist.internal.ui.SAVE_SELECTION
import ru.pavlig43.itemlist.internal.ui.SELECTION

@Composable
internal fun SelectionLogic(
    fullListSelection: List<ItemType>,
    saveSelection: (List<ItemType>) -> Unit
) {

    var isMenuOpen by remember { mutableStateOf(false) }

    val selectedItems = remember { mutableStateListOf<ItemType>() }
    SettingsToolTip(
        tooltipText = SELECTION,
        onClick = { isMenuOpen = !isMenuOpen },
        icon = if (selectedItems.isEmpty()) Icons.Sharp.FilterAltOff else Icons.Sharp.FilterAlt,
        badge = { if (selectedItems.isNotEmpty()) Badge() }
    )
    DropdownMenu(
        expanded = isMenuOpen,
        onDismissRequest = { isMenuOpen = false }
    ) {
        DropDownBody(
            items = fullListSelection,
            selectedItems = selectedItems,
            addItemInSelection = { selectedItems.add(it)},
            removeItemFromSelection = { selectedItems.remove(it)},
            saveSelection = {
                isMenuOpen = false
                saveSelection(selectedItems.toList())
            }
        )

    }
}
@Suppress("LongParameterList")
@Composable
private fun DropDownBody(
    items: List<ItemType>,
    selectedItems: List<ItemType>,
    addItemInSelection: (ItemType) -> Unit,
    removeItemFromSelection: (ItemType) -> Unit,
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
            Text(SAVE_SELECTION)
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
            .fillMaxWidth().clickable{onCheckedChange(!isChecked)},
        verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
        Text(item.displayName)
    }
}

