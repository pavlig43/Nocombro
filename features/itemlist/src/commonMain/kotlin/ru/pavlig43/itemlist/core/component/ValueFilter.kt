package ru.pavlig43.itemlist.core.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class ValueFilterComponent<V>(
    componentContext: ComponentContext,
    val initialValue: V
) : ComponentContext by componentContext {

    private val _valueFlow = MutableStateFlow(initialValue)
    val valueFlow: StateFlow<V> = _valueFlow.asStateFlow()

    fun onChange(new: V) {
        _valueFlow.update { new }
    }
}
