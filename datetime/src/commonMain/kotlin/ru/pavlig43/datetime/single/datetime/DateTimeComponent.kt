package ru.pavlig43.datetime.single.datetime

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDateTime

class DateTimeComponent(
    componentContext: ComponentContext,
    initDatetime: LocalDateTime,
    val onChangeDate: (LocalDateTime) -> Unit,
    val onDismissRequest:()-> Unit
) : ComponentContext by componentContext {
    private val _dateTime = MutableStateFlow(initDatetime)
    val dateTime = _dateTime.asStateFlow()

}