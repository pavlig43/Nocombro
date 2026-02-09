package ru.pavlig43.mutable.api.component.singleLine

import ru.pavlig43.tablecore.model.ITableUi

sealed interface SingleLineUiEvent {
    data class UpdateItem(val item: ITableUi): SingleLineUiEvent
}