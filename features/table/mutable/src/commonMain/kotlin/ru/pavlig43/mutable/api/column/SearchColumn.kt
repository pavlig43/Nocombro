package ru.pavlig43.mutable.api.column

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.search
import ua.wwind.table.EditableColumnBuilder
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType

/**
 * Колонка с текстом и иконкой поиска для открытия диалога выбора.
 *
 * @param headerText Заголовок колонки
 * @param column Ключ колонки
 * @param valueOf Функция получения значения из элемента
 * @param onOpenDialog Callback для открытия диалога выбора (принимает элемент таблицы)
 * @param filterType Опциональный тип фильтра для колонки
 * @param alignment Выравнивание содержимого
 */

@Suppress("LongParameterList")
fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.textWithSearchIconColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> String,
    onOpenDialog: (T) -> Unit,
    filterType: TableFilterType.TextTableFilter? = null,
    alignment: Alignment = Alignment.Center
) {
    column(column, valueOf = valueOf) {
        autoWidth(300.dp)
        header(headerText)
        align(alignment)
        filterType?.let {
            filter(it)
        }
        textWithSearchIconCell(
            valueOf = valueOf,
            onOpenDialog = onOpenDialog
        )
        sortable()
    }
}

private fun <T : Any, C, E> EditableColumnBuilder<T, C, E>.textWithSearchIconCell(
    valueOf: (T) -> String,
    onOpenDialog: (T) -> Unit,
) {
    cell { item, _ ->
        NameRowWithSearchIcon(
            text = valueOf(item),
            onOpenChooseDialog = { onOpenDialog(item) }
        )
    }
}
@Composable
private fun NameRowWithSearchIcon(
    text: String,
    onOpenChooseDialog: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text, Modifier.weight(1f).padding(horizontal = 4.dp))
        ToolTipIconButton(
            tooltipText = "Выбрать",
            onClick = onOpenChooseDialog,
            icon = Res.drawable.search,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}