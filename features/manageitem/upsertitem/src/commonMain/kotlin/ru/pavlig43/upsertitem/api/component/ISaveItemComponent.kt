package ru.pavlig43.upsertitem.api.component

import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.core.data.Item

interface ISaveItemComponent<I: Item> {

    val isValidValue:StateFlow<Boolean>
    val onSuccessAction:()->Unit
    fun saveItem()
    val saveState:StateFlow<SaveItemState>
}
sealed interface SaveItemState{
    class Init :SaveItemState
    class Loading :SaveItemState
    class Success :SaveItemState
    class Error(val message:String):SaveItemState
}