package ru.pavlig43.itemlist.refactor

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.itemlist.core.data.IItemUi
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi

@Immutable
data class DocumentTableData(

    override val displayedItems: List<DocumentItemUi> = emptyList(),
    /** IDs of selected  */
    override val selectedIds: Set<Int> = emptySet(),

    /** Whether selection mode is enabled */

    override val isSelectionMode: Boolean,
) : TableData<DocumentItemUi>

interface TableData<I : IItemUi> {
    val displayedItems: List<I>
    val selectedIds: Set<Int>

    val isSelectionMode: Boolean

}
@Immutable
data class TableData1<I: IItemUi>(

    val displayedItems: List<I> = emptyList(),
    /** IDs of selected  */
    val selectedIds: Set<Int> = emptySet(),

    /** Whether selection mode is enabled */

    val isSelectionMode: Boolean,
)


interface IImmutableTableComponent<I : IItemUi> {
    val itemListState: StateFlow<ItemListState1<I>>

    val tableData: StateFlow<TableData<I>>
    fun onEvent(event: SampleUiEvent)
}

sealed interface ItemListState1<out O : Any> {
    class Loading : ItemListState1<Nothing>
    class Success<O : Any>(val data: List<O>) : ItemListState1<O>
    class Error<O : Any>(val message: String) : ItemListState1<O>
}


fun <BD, UI : IItemUi> RequestResult<List<BD>>.toItemListState(mapper: BD.() -> UI): ItemListState1<UI> {
    return when (this) {
        is RequestResult.Error -> ItemListState1.Error(message ?: "unknown error")
        is RequestResult.InProgress -> ItemListState1.Loading()
        is RequestResult.Initial -> ItemListState1.Loading()
        is RequestResult.Success<List<BD>> -> ItemListState1.Success(data.map(mapper))
    }
}