package ru.pavlig43.transaction.internal.plan

import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.idWithSelection
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineReferenceField
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.editableTableColumns

internal enum class PlanLineField { SELECTION, NUMBER, PRODUCT, PLANNED, PACKAGE_WEIGHT }

internal fun productPlanLineColumns(
    onEvent: (MutableUiEvent) -> Unit,
    onPickProduct: (String) -> Unit,
    onOpenProduct: (Int) -> Unit,
) = editableTableColumns<PlanLineRow, PlanLineField, TableData<PlanLineRow>> {
    idWithSelection(selectionKey = PlanLineField.SELECTION, idKey = PlanLineField.NUMBER, onEvent = onEvent)
    column(PlanLineField.PRODUCT, valueOf = { it.line.productName }) {
        header("Продукт")
        autoWidth()
        cell { row, _ ->
            SingleLineReferenceField(
                value = row.line.productName.ifBlank { "Выбрать продукт" },
                onSelect = { onPickProduct(row.line.syncId) },
                onOpenSelected = { onOpenProduct(row.line.productId) },
                selectEnabled = true,
                openEnabled = row.line.productId != 0,
            )
        }
    }
    decimalColumn(
        key = PlanLineField.PLANNED,
        getValue = { DecimalData3(parseGrams(it.line.planned) ?: 0L) },
        headerText = "План, кг",
        updateItem = { row, weight ->
            onEvent(MutableUiEvent.UpdateItem(row.copy(line = row.line.copy(planned = kgText(weight.value)))))
        },
        isSortable = false,
    )
    decimalColumn(
        key = PlanLineField.PACKAGE_WEIGHT,
        getValue = { DecimalData3(parseGrams(it.line.packageWeight) ?: 0L) },
        headerText = "Пакет, кг",
        updateItem = { row, weight ->
            onEvent(MutableUiEvent.UpdateItem(row.copy(line = row.line.copy(packageWeight = kgText(weight.value)))))
        },
        isSortable = false,
    )
}
