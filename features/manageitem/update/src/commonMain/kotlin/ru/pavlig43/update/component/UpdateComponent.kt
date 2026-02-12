package ru.pavlig43.update.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.componentCoroutineScope

/**
 * @param[message] строковое обозначение проблемы(например пустое имя, отсутствие файлов)
 * @param[onSelectProblemTab] делает активной вкладку, на которой проблема
 */
data class ErrorMessage(
    val message: String,
    val onSelectProblemTab: () -> Unit,
)


/**
 * Компонент, который привязан к кнопке обновления и обновляет все вкладки в форме объекта
 * @param [errorMessages] показывает список невалидных заполненных полей(например пустое имя, отсутствие файлов)
 * @param [closeFormScreen] при успешном обновлении закрывает экран
 */
class UpdateComponent(
    componentContext: ComponentContext,
    private val onUpdateAllTabs: suspend () -> Result<Unit>,
    errorMessages: Flow<List<ErrorMessage>>,
    val closeFormScreen: () -> Unit,
) : ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()

    val isValidValue: StateFlow<List<ErrorMessage>> = errorMessages
        .stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            emptyList()
        )

     fun onUpdate() {
        _updateState.update { UpdateState.Loading }
        coroutineScope.launch {
            val result = onUpdateAllTabs().fold(
                onSuccess = { UpdateState.Success},
                onFailure = { UpdateState.Error(it.message ?: "Неизвестная ошибка")}
            )
            _updateState.update { result }
        }

    }

    private val _updateState: MutableStateFlow<UpdateState> = MutableStateFlow(UpdateState.Init)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

}

