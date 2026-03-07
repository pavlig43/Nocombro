package ru.pavlig43.datetime.single.date

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

class DateComponent(
    componentContext: ComponentContext,
    initDate: LocalDate,
    val onChangeDate: (LocalDate) -> Unit,
    val onDismissRequest:()-> Unit
) : ComponentContext by componentContext {
    private val _date = MutableStateFlow(initDate)
    val date = _date.asStateFlow()

}