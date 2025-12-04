package ru.pavlig43.itemlist.internal

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.data.ItemType

internal sealed class ItemFilter(val componentName: String) {
    data class Type<I : ItemType>(val value: List<I>) : ItemFilter("types")
    data class SearchText(val value: String) : ItemFilter("search_text")
}


internal fun <T : ItemFilter> ComponentContext.generateComponent(filter: T): BaseFilterComponent<T> {
    val context = childContext(filter.componentName)
    return BaseFilterComponent(context, filter)
}


internal class BaseFilterComponent<T : ItemFilter>(
    componentContext: ComponentContext,
    initialValue: T
) : ComponentContext by componentContext {

    private val _filterFlow = MutableStateFlow(initialValue)
    val filterFlow: StateFlow<T> = _filterFlow.asStateFlow()

    fun onChangeFilter(new: T) {
        _filterFlow.update { new }
    }

}