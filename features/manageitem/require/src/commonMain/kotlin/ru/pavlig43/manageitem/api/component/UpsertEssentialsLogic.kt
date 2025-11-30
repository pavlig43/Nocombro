package ru.pavlig43.manageitem.api.component

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
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.data.UpsertState

//class UpsertEssentialsLogic<I: GenericItem>(
//    componentContext: ComponentContext,
//    private val onUpsertItem:suspend () -> RequestResult<Int>,
//    val onSuccessUpsert: (id:Int) -> Unit,
//    otherValidFields: Flow<Boolean>,
//): ComponentContext by componentContext{
//
//    private val coroutineScope = componentCoroutineScope()
//    private val _upsertState: MutableStateFlow<UpsertState<I>> =
//        MutableStateFlow(UpsertState.Init)
//    val upsertState = _upsertState.asStateFlow()
//
//    val isValidValue: StateFlow<Boolean> =
//        otherValidFields.combine(_upsertState) { other, state ->
//            other && (  state is UpsertState.Init || state is UpsertState.Error)
//        }.stateIn(
//            coroutineScope,
//            SharingStarted.Eagerly,
//            false
//        )
//
//    fun upsert(){
//        coroutineScope.launch {
//            _upsertState.update { UpsertState.Loading }
//            _upsertState.update { onUpsertItem().toSaveStateItem() }
//            _upsertState.value.let { state ->
//                if (state is UpsertState.Success) {
//                    onSuccessUpsert(state.id)
//                }
//            }
//        }
//    }
//}
//class UpsertEssentialsLogic1<I: Any>(
//    componentContext: ComponentContext,
//    private val onUpsertItem:suspend () -> RequestResult<Int>,
//    val onSuccessUpsert: (id:Int) -> Unit,
//    otherValidFields: Flow<Boolean>,
//): ComponentContext by componentContext{
//
//    private val coroutineScope = componentCoroutineScope()
//    private val _upsertState: MutableStateFlow<UpsertState1<I>> =
//        MutableStateFlow(UpsertState1.Init)
//    val upsertState = _upsertState.asStateFlow()
//
//    val isValidValue: StateFlow<Boolean> =
//        otherValidFields.combine(_upsertState) { other, state ->
//            other && (  state is UpsertState1.Init || state is UpsertState1.Error)
//        }.stateIn(
//            coroutineScope,
//            SharingStarted.Eagerly,
//            false
//        )
//
//    fun upsert(){
//        coroutineScope.launch {
//            _upsertState.update { UpsertState1.Loading }
//            _upsertState.update { onUpsertItem().toSaveStateItem1() }
//            _upsertState.value.let { state ->
//                if (state is UpsertState1.Success) {
//                    onSuccessUpsert(state.id)
//                }
//            }
//        }
//    }
//}
//sealed interface UpsertState1<out O: Any> {
//    object Init : UpsertState1<Nothing>
//    object Loading : UpsertState1<Nothing>
//    data class Success<O : Any>(val id: Int) : UpsertState1<O>
//    data class Error(val message: String) : UpsertState1<Nothing>
//}
//
//private fun <O: Any> RequestResult<Int>.toSaveStateItem1(): UpsertState1<O> {
//    return when (this) {
//        is RequestResult.Error<*> -> UpsertState1.Error(this.message ?: "Неизвестная ошибка")
//        is RequestResult.InProgress -> UpsertState1.Loading
//        is RequestResult.Initial<*> -> UpsertState1.Init
//        is RequestResult.Success<Int> -> UpsertState1.Success(data)
//    }
//}
//private fun <O: GenericItem> RequestResult<Int>.toSaveStateItem(): UpsertState<O> {
//    return when (this) {
//        is RequestResult.Error<*> -> UpsertState.Error(this.message ?: "Неизвестная ошибка")
//        is RequestResult.InProgress -> UpsertState.Loading
//        is RequestResult.Initial<*> -> UpsertState.Init
//        is RequestResult.Success<Int> -> UpsertState.Success(data)
//    }
//}