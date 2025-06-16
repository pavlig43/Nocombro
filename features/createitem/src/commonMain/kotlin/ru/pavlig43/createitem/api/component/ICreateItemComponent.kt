package ru.pavlig43.createitem.api.component


import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.createitem.internal.ui.NAME_ALREADY_EXISTS_MESSAGE
import ru.pavlig43.createitem.internal.ui.NAME_IS_EMPTY_MESSAGE
import ru.pavlig43.database.data.common.data.ItemType

interface ICreateItemComponent {
    val name:StateFlow<String>
    fun onNameChange(name:String)
    val type:StateFlow<ItemType?>

    val isValidName:StateFlow<ValidNameState>
}
sealed class ValidNameState{
    class Initial:ValidNameState()
    class Valid:ValidNameState()
    class Empty(val message:String):ValidNameState()
    class Error(val message:String):ValidNameState()
    class AllReadyExists(val message:String):ValidNameState()
}
