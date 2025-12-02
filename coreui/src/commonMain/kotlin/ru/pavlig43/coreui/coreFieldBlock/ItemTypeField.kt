package ru.pavlig43.coreui.coreFieldBlock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.data.ItemType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <I: ItemType> ItemTypeField(
    currentType:I?,
    typeVariants: List<I>,
    onChangeType:(I?)-> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { !expanded },
        modifier = modifier
    ) {
        Column {
            Text(text = "Тип объекта")
            OutlinedCard(
                modifier = Modifier.height(56.dp).width(200.dp)
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (currentType == null) Arrangement.End else Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize(),

                    ) {
                    Text(
                        currentType?.displayName?:"",
                        Modifier.padding(start = 12.dp),
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        Modifier.size(32.dp)
                    )


                }
            }
            if (currentType == null) {
                Text(
                    text = "Необходимо выбрать тип",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.Companion.exposedDropdownSize()
        ) {
            typeVariants.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onChangeType(type)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }


}