package ru.pavlig43.flowImmutable.api.component

import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.IMultiLineTableUi

/**
 * События, которые могут происходить в таблице с многострочными данными.
 *
 * Используется в [FlowMultilineComponent] для обработки пользовательских взаимодействий.
 */
sealed interface FlowMultiLineEvent {
    /**
     * Событие клика по строке таблицы.
     *
     * @param I Тип UI элемента строки
     * @param item Элемент строки, по которому кликнули
     */
    data class RowClick<I: IMultiLineTableUi>(val item: I) : FlowMultiLineEvent

    /**
     * Событие связанное с выбором строк (selection).
     *
     * @param selectionUiEvent Детальное событие выбора
     */
    data class Selection(val selectionUiEvent: SelectionUiEvent) : FlowMultiLineEvent

    /**
     * Событие удаления выбранных строк.
     *
     * Удаляет все строки, находящиеся в текущем выделении.
     */
    data object DeleteSelected : FlowMultiLineEvent
}