package ru.pavlig43.createitem.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ru.pavlig43.createitem.api.component.ICreateItemComponent
import ru.pavlig43.createitem.api.component.ValidNameState
import ru.pavlig43.createitem.internal.ui.NAME
import ru.pavlig43.createitem.internal.ui.OBJECT_TYPE
import ru.pavlig43.createitem.internal.ui.OBJECT_TYPE_IS_EMPTY_MESSAGE
import ru.pavlig43.database.data.common.data.ItemType

@Composable
fun CreateBaseRowsOfItem(
    component: ICreateItemComponent,
    modifier: Modifier = Modifier
) {
    val name by component.name.collectAsState()
    val type: ItemType? by component.type.collectAsState()
    val typeVariants by component.typeVariants.collectAsState()
    val validNameState by component.isValidName.collectAsState()
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        NameField(
            name = name,
            onChangeName = component::onNameChange,
            validNameState = validNameState
        )
        SelectItemType(
            typeVariants = typeVariants,
            currentType = type,
            onSelectType = component::onSelectType
        )


    }
}

@Composable
private fun NameField(
    name: String,
    onChangeName: (String) -> Unit,
    validNameState: ValidNameState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = NAME)
        val keyboardController = LocalSoftwareKeyboardController.current
        TextField(
            value = name,
            onValueChange = onChangeName,
            isError = validNameState !is ValidNameState.Valid,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                }
            )
        )
        Text(
            text = when (validNameState) {
                is ValidNameState.Empty -> validNameState.message
                is ValidNameState.Error -> validNameState.message
                is ValidNameState.Initial -> ""
                is ValidNameState.Valid -> ""
                is ValidNameState.AllReadyExists -> validNameState.message
            },
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
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