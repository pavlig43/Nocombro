package ru.pavlig43.transaction.internal.multiopzs

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Checkbox
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.mutable.api.column.NameRowWithSearchIcon
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.readTextColumn
import ru.pavlig43.mutable.api.column.idWithSelection
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.add_circle
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.editableTableColumns

internal enum class MultiOpzsField { SELECTION, COMPOSE_ID, PRODUCT, DECLARATION, VENDOR, COUNT, RESULT }

/** Штатные таблица и редактор веса; порядок строк всегда совпадает с порядком создания. */
internal fun multiOpzsColumns(component: MultiOpzsComponent, running: Boolean) =
    editableTableColumns<MultiOpzsRow, MultiOpzsField, TableData<MultiOpzsRow>> {
        idWithSelection(
            selectionKey = MultiOpzsField.SELECTION,
            idKey = MultiOpzsField.COMPOSE_ID,
            onEvent = component::onTableEvent,
        )
        column(MultiOpzsField.PRODUCT, valueOf = MultiOpzsRow::productName) {
            header("Продукт")
            width(180.dp, 270.dp)
            cell { row, _ ->
                if (running || row.result is OpzsRowResult.Created) {
                    Text(row.productName)
                } else {
                    NameRowWithSearchIcon(
                        text = row.productName.ifEmpty { "Выбрать продукт" },
                        onClick = { component.openProduct(row.syncId) },
                    )
                }
            }
        }
        column(MultiOpzsField.DECLARATION, valueOf = MultiOpzsRow::declarationName) {
            header("Декларация")
            width(180.dp, 260.dp)
            cell { row, _ ->
                val text = if (row.loading) "Загрузка…" else row.declarationName.ifEmpty { "Выбрать декларацию" }
                if (running || row.result is OpzsRowResult.Created || row.loading || row.productId == 0) {
                    Text(text)
                } else {
                    NameRowWithSearchIcon(text = text, onClick = { component.openDeclaration(row.syncId) })
                }
            }
        }
        readTextColumn(
            headerText = "Поставщик", column = MultiOpzsField.VENDOR,
            valueOf = MultiOpzsRow::vendorName, isSortable = false,
        )
        decimalColumn(
            key = MultiOpzsField.COUNT, getValue = MultiOpzsRow::count, headerText = "Кг",
            updateItem = { row, count -> component.changeCount(row.syncId, count) },
            isSortable = false,
        )
        column(MultiOpzsField.RESULT, valueOf = { it.result.displayText() }) {
            header("Результат")
            width(240.dp, 350.dp)
            rowHeight(min = 48.dp)
            cell { row, _ ->
                Text(
                    text = row.result.displayText(),
                    color = if (row.result is OpzsRowResult.Failed) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }.map { column ->
        if (column.key == MultiOpzsField.SELECTION) {
            column.copy(
                title = if (running) ({
                    IconButton(onClick = {}, enabled = false) {
                        Icon(painterResource(Res.drawable.add_circle), contentDescription = "Добавить")
                    }
                    ""
                }) else column.title,
                cell = { row, data ->
                    if (!running && row.result !is OpzsRowResult.Created) {
                        Checkbox(
                            checked = row.composeId in data.selectedIds,
                            onCheckedChange = {
                                component.onTableEvent(MutableUiEvent.Selection(SelectionUiEvent.ToggleSelection(row.composeId)))
                            },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                },
            )
        } else if (column.key == MultiOpzsField.VENDOR) {
            column.copy(width = 260.dp, minWidth = 180.dp, autoWidth = false)
        } else if (column.key != MultiOpzsField.COUNT) column else {
            val editor = column.editCell
            column.copy(
                width = 110.dp, minWidth = 90.dp, autoWidth = false,
                canStartEdit = { row, _ -> !running && row.result !is OpzsRowResult.Created },
                editCell = { row, data, onComplete ->
                    if (running || row.result is OpzsRowResult.Created) Text(row.count.toString())
                    else editor?.invoke(this, row, data, onComplete)
                },
            )
        }
    }.toImmutableList()

private fun OpzsRowResult.displayText(): String = when (this) {
    OpzsRowResult.Empty -> "—"
    OpzsRowResult.Waiting -> "Ожидает"
    OpzsRowResult.Creating -> "Создаётся…"
    is OpzsRowResult.Created -> "ОПЗС №$transactionId создана и проведена"
    is OpzsRowResult.Failed -> reason
}
