package ru.pavlig43.manageitem.api.ui

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
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.coreui.StringColumnField
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen
import ru.pavlig43.manageitem.api.component.RequireValuesSlotComponent
import ru.pavlig43.manageitem.api.data.DefaultRequireValues
import ru.pavlig43.manageitem.internal.ui.COMMENT
import ru.pavlig43.manageitem.internal.ui.NAME
import ru.pavlig43.manageitem.internal.ui.OBJECT_TYPE
import ru.pavlig43.manageitem.internal.ui.OBJECT_TYPE_IS_EMPTY_MESSAGE


@Composable
fun RequireValuesScreen(
    component: RequireValuesSlotComponent<out Item,out ItemType>,
    modifier: Modifier = Modifier
) {
    val requireValues by component.requireValues .collectAsState()
    val typeVariants by component.typeVariants.collectAsState()

    Column(modifier) {
        LoadInitDataScreen(
            component.initComponent
        ){
            RequireValuesBody(
                requireValues = requireValues,
                onChangeName = component::onNameChange,
                typeVariants = typeVariants,
                onSelectType = component::onSelectType,
                onChangeComment = component::onCommentChange
            )

        }
    }

}

@Suppress("LongParameterList")
@Composable
private fun RequireValuesBody(
    requireValues: DefaultRequireValues,
    onChangeName: (String) -> Unit,
    onChangeComment:(String)->Unit,
    typeVariants: List<ItemType>,
    onSelectType: (ItemType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StringColumnField(
                value = requireValues.name,
                onValueChange = onChangeName,
                headText = NAME
            )
            SelectItemType(
                typeVariants = typeVariants,
                currentType = requireValues.type,
                onSelectType = onSelectType
            )
        }
        StringColumnField(
            value = requireValues.comment,
            onValueChange = onChangeComment,
            headText = COMMENT,
            modifier = Modifier.fillMaxWidth()

        )
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectItemType(
    currentType: ItemType?,
    typeVariants: List<ItemType>,
    onSelectType: (ItemType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        Column {
            Text(text = OBJECT_TYPE)
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
                        currentType?.displayName ?: "",
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
                    text = OBJECT_TYPE_IS_EMPTY_MESSAGE,
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
                        onSelectType(type)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }


}