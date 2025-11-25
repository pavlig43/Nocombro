package ru.pavlig43.itemlist.api.component


import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.core.DeleteState
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.itemlist.api.data.ItemUi

interface IItemListComponent {
    val itemListState: StateFlow<ItemListState>
    val deleteState: StateFlow<DeleteState>
    val selectedItemIds: List<Int>
    val onCreate:()->Unit
    fun saveSelection(selection: List<ItemType>)
    val fullListSelection: List<ItemType>
    fun deleteItems(ids: List<Int>)
    fun shareItems(ids: List<Int>)
    fun actionInSelectedItemIds(checked: Boolean, id: Int)
    val onItemClick:(IItemUi) -> Unit
    val searchField:StateFlow<String>
    fun onSearchChange(value:String)

    /**
     * Если да, то будет чекбоксы у каждого элемента, при выборе хотя бы одного будет появляться строка с возможностью удаления и поделиться
     */
    val withCheckbox:Boolean
}

sealed interface ItemListState {
    class Initial : ItemListState
    class Loading : ItemListState
    class Success(val data: List<ItemUi>) : ItemListState
    class Error(val message: String) : ItemListState
}
sealed interface ItemListState1<out O : IItemUi> {
    class Initial : ItemListState1<Nothing>
    class Loading : ItemListState1<Nothing>
    class Success<O : IItemUi>(val data: List<O>) : ItemListState1<O>
    class Error(val message: String) : ItemListState1<Nothing>
}

