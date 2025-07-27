package ru.pavlig43.upsertitem.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.Item
import ru.pavlig43.upsertitem.api.data.ItemsForUpsert
import ru.pavlig43.upsertitem.data.ISaveItemRepository

class SaveItemComponent<I : Item>(
    componentContext: ComponentContext,
    isOtherValidValue: Flow<Boolean>,
    private val getItems:()-> ItemsForUpsert<I>,
    override val onSuccessAction: () -> Unit,
    private val saveItemRepository: ISaveItemRepository<I>,

    ) : ComponentContext by componentContext, ISaveItemComponent<I> {
    private val coroutineScope = componentCoroutineScope()

    private val _saveState = MutableStateFlow<SaveItemState>(SaveItemState.Init())

    override val saveState: StateFlow<SaveItemState> = _saveState.asStateFlow()
    override val isValidValue: StateFlow<Boolean> =
        isOtherValidValue.combine(_saveState) { otherValidValue, state ->
            otherValidValue && (state is SaveItemState.Init || state is SaveItemState.Error)
        }.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            false
        )

    override fun saveItem() {
        val items = getItems()
        coroutineScope.launch {
            _saveState.update { SaveItemState.Loading() }
            val result = saveItemRepository.saveItem(items.newItem,items.initItem)
            _saveState.update { result.toSaveStateItem() }
        }

    }


}
private fun RequestResult<Unit>.toSaveStateItem(): SaveItemState {
    return when(this){
        is RequestResult.Error<*> -> SaveItemState.Error(this.message ?: "Неизвестная ошибка")
        is RequestResult.InProgress -> SaveItemState.Loading()
        is RequestResult.Initial<*> -> SaveItemState.Init()
        is RequestResult.Success<*> -> SaveItemState.Success()
    }
}
