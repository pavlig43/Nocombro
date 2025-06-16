package ru.pavlig43.itemlist.api.component


import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.database.data.common.data.ItemType
import ru.pavlig43.itemlist.api.data.ItemUi

interface IItemListComponent {
    val itemListState: StateFlow<ItemListState>
    val deleteState: StateFlow<DeleteState>
    val selectedItemIds: List<Int>
    fun onCreate()
    fun saveSelection(selection: List<ItemType>)
    val fullListSelection: List<ItemType>
    fun deleteItems(ids: List<Int>)
    fun shareItems(ids: List<Int>)
    fun actionInSelectedItemIds(checked: Boolean, id: Int)
}

sealed interface ItemListState {
    class Initial : ItemListState
    class Loading : ItemListState
    class Success(val data: List<ItemUi>) : ItemListState
    class Error(val message: String) : ItemListState
}
sealed interface DeleteState{
    class Initial : DeleteState
    class Loading : DeleteState
    class Success(val message: String) : DeleteState
    class Error(val message: String) : DeleteState

}