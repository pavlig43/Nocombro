package ru.pavlig43.flowImmutable.api.component.column

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.flowImmutable.api.component.FlowMultiLineEvent
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.ui.createButtonNew
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.ReadonlyTableColumnsBuilder

/**
 * Создаёт колонку с чекбоксами для выбора строк и колонку с ID.
 *
 * Добавляет в таблицу:
 * - Колонку выбора с чекбоксами и кнопкой добавления в заголовке
 * - Колонку с идентификаторами строк
 *
 * @param T Тип UI элемента таблицы
 * @param C Тип ключей колонок
 * @param E Тип данных таблицы
 * @param selectionKey Ключ для колонки выбора
 * @param idKey Ключ для колонки ID
 * @param onCallAddDialog Callback при нажатии кнопки добавления
 * @param onEvent Callback для отправки событий таблицы
 */
fun<T: IMultiLineTableUi,C,E: TableData<T>> ReadonlyTableColumnsBuilder<T, C, E>.idWithSelection(
    selectionKey:C,
    idKey:C,
    onCallAddDialog:()->Unit,
    onEvent:(FlowMultiLineEvent)-> Unit,
){
    coreIdWithSelection(
        selectionKey = selectionKey,
        idKey = idKey,
        onAdd = onCallAddDialog,
        onSelectionUiEvent = {selectionUiEvent -> onEvent(FlowMultiLineEvent.Selection(selectionUiEvent))}
    )
}
private fun <T : IMultiLineTableUi, C, E : TableData<T>> ReadonlyTableColumnsBuilder<T, C, E>.coreIdWithSelection(
    selectionKey: C,
    idKey: C,
    onAdd: () -> Unit,
    onSelectionUiEvent: (SelectionUiEvent) -> Unit,


    ) {

    column(selectionKey, { it.composeId }) {
        coreSelectionCell(onAdd, onSelectionUiEvent)
    }
    column(idKey, valueOf = { it.composeId }) {
        header("Ид")
        align(Alignment.Center)
        cell { item, _ ->
            Text(
                text = item.composeId.toString(),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
        autoWidth(max = 500.dp)

    }
}
private fun <T : IMultiLineTableUi, C, E : TableData<T>> ReadonlyColumnBuilder<T, C, E>.coreSelectionCell(
    onAdd: () -> Unit,
    onSelectionUiEvent: (SelectionUiEvent) -> Unit
) {

    width(48.dp)
    autoWidth(48.dp)
    header {
        createButtonNew {
            onAdd()
        }
    }

    cell { item, tableData ->
        if (tableData.isSelectionMode){
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Checkbox(
                    checked = item.composeId in tableData.selectedIds,
                    onCheckedChange = {
                        onSelectionUiEvent(
                            SelectionUiEvent.ToggleSelection(
                                item.composeId
                            )
                        )
                    },
                )
            }

        }

    }
}