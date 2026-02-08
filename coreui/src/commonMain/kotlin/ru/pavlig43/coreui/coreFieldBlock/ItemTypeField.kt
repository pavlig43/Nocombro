package ru.pavlig43.coreui.coreFieldBlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.core.model.ItemType
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.lock

@Composable
fun <I : ItemType> ReadWriteItemTypeField(
    readOnly: Boolean,
    currentType: I?,
    typeVariants: List<I>,
    onChangeType: (I?) -> Unit,
) {
    if (readOnly) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Icon(
                painter = painterResource(Res.drawable.lock),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                currentType?.displayName ?: "*", Modifier.padding(vertical = 16.dp)
            )
        }

    } else {
        ItemTypeField(
            currentType = currentType, typeVariants = typeVariants, onChangeType = onChangeType
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <I : ItemType> ItemTypeField(
    currentType: I?,
    typeVariants: List<I>,
    onChangeType: (I?) -> Unit,
    modifier: Modifier = Modifier
) {

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.menuAnchor(
                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    true
                ).padding(horizontal = 12.dp)
            ) {
                val text = currentType?.displayName ?: "Необходимо выбрать тип"
                Text(
                    text,
                )
            }


        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(IntrinsicSize.Max)
        ) {
            typeVariants.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = type.displayName, maxLines = 1
                        )
                    }, onClick = {
                        onChangeType(type)
                        expanded = false
                    }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }


}