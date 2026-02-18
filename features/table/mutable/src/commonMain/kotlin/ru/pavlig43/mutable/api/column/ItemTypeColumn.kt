package ru.pavlig43.mutable.api.column

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import ru.pavlig43.theme.arrow_downward
import ru.pavlig43.theme.lock
import ua.wwind.table.EditableColumnBuilder
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType

@Suppress("LongParameterList")
fun <T : Any, C, E, Type : ItemType> EditableTableColumnsBuilder<T, C, E>.writeItemTypeColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> Type?,
    options: List<Type>,
    onTypeSelected: (T, Type) -> Unit,
    filterType: TableFilterType<*>? = null,
    alignment: Alignment = Alignment.Center
) {
    column(column, valueOf = valueOf) {
        header(headerText)
        autoWidth(300.dp)
        align(alignment)
        filterType?.let {
            filter(it)
        }
        writeItemTypeCell(
            valueOf = valueOf,
            options = options,
            onTypeSelected = onTypeSelected
        )
        sortable()
    }
}

fun <T : Any, C, E, Type : ItemType> EditableTableColumnsBuilder<T, C, E>.readItemTypeColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> Type?,
    filterType: TableFilterType<*>? = null,
    alignment: Alignment = Alignment.Center
) {
    column(column, valueOf = valueOf) {
        autoWidth(300.dp)
        header(headerText)
        align(alignment)
        filterType?.let {
            filter(it)
        }
        readItemTypeCell(valueOf = valueOf)
        sortable()
    }
}

private fun <T : Any, C, E, Type : ItemType> EditableColumnBuilder<T, C, E>.writeItemTypeCell(
    valueOf: (T) -> Type?,
    options: List<Type>,
    onTypeSelected: (T, Type) -> Unit,
) {
    cell { item, _ ->
        WriteItemType(
            currentType = valueOf(item),
            options = options,
            onTypeSelected = { type -> onTypeSelected(item, type) }
        )
    }
}

private fun <T : Any, C, E, Type : ItemType> EditableColumnBuilder<T, C, E>.readItemTypeCell(
    valueOf: (T) -> Type?,
) {
    cell { item, _ ->
        ReadItemType(valueOf(item))
    }
}

// Public UI компоненты для использования в других местах
@Composable
fun <Type : ItemType> ReadItemType(currentType: Type?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(12.dp)
    ) {
        Icon(
            painter = painterResource(Res.drawable.lock),
            contentDescription = "Только чтение",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(currentType?.displayName ?: "*")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <Type : ItemType> WriteItemType(
    currentType: Type?,
    options: List<Type>,
    onTypeSelected: (Type) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val text = currentType?.displayName ?: "Необходимо выбрать тип"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(Res.drawable.arrow_downward),
                contentDescription = "Выбрать тип",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(IntrinsicSize.Max)
        ) {
            options.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = type.displayName,
                            maxLines = 1
                        )
                    },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
