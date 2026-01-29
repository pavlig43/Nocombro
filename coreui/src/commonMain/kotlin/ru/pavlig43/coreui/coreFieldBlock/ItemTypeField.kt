package ru.pavlig43.coreui.coreFieldBlock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.core.model.ItemType
import ru.pavlig43.theme.arrow_downward
import ru.pavlig43.theme.arrow_upward
import ru.pavlig43.theme.Res

@Composable
fun<I:ItemType> ReadWriteItemTypeField(
    readOnly: Boolean,
    currentType:I?,
    typeVariants: List<I>,
    onChangeType:(I?)-> Unit,
){
    if (readOnly){
        Card(Modifier.padding(vertical = 16.dp)) {
            Text(currentType?.displayName ?: "*", Modifier.padding(4.dp))
        }
    }
    else{
        ItemTypeField(
            currentType = currentType,
            typeVariants = typeVariants,
            onChangeType = onChangeType
        )
}
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <I: ItemType> ItemTypeField(
    currentType:I?,
    typeVariants: List<I>,
    onChangeType:(I?)-> Unit,
    modifier: Modifier = Modifier
) {

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        Column {
            Text(text = "Тип объекта")
            OutlinedCard(
                modifier = Modifier.height(56.dp).width(200.dp)
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
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
                        painter = painterResource(if (expanded) Res.drawable.arrow_upward else Res.drawable.arrow_downward),
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
            modifier = Modifier.exposedDropdownSize()
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