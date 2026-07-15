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
 * Управляет сохранением всех вкладок формы и отдельным шагом постобработки.
 *
 * Повторный клик во время [UpdateState.Loading] игнорируется. Ошибка сохранения
 * переводит компонент в [UpdateState.Error]. Если данные уже сохранены, но пересчёт
 * или иная постобработка упала, используется [UpdateState.PostProcessError]: её
 * повтор не вызывает [onUpdateAllTabs] второй раз.
 *
 * @param onUpdateAllTabs атомарное сохранение данных вкладок.
 * @param errorMessages поток ошибок полей, блокирующих обычное сохранение.
 * @param postProcessAfterUpdate действие после успешной записи, которое можно повторить отдельно.
 */
class UpdateComponent(
    componentContext: ComponentContext,
    private val onUpdateAllTabs: suspend () -> Result<Unit>,
    errorMessages: Flow<List<ErrorMessage>>,
    private val postProcessAfterUpdate: suspend () -> Unit = {},
) : ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()

    val isValidValue: StateFlow<List<ErrorMessage>> = errorMessages
        .stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    /** Запускает сохранение и затем постобработку, не допуская параллельного повтора. */
    fun onUpdate() {
        if (_updateState.value == UpdateState.Loading) return
        _updateState.value = UpdateState.Loading
        coroutineScope.launch {
            val updateResult = runCatching {
                onUpdateAllTabs()
            }.getOrElse { error -> Result.failure(error) }
            _updateState.value = updateResult.fold(
                onSuccess = { runPostProcess() },
                onFailure = { UpdateState.Error(it.message ?: "Не удалось сохранить данные") },
            )
        }
    }

    /**
     * Повторяет только постобработку после уже успешного сохранения данных.
     *
     * В других состояниях вызов ничего не делает.
     */
    fun retryPostProcess() {
        if (_updateState.value !is UpdateState.PostProcessError) return
        _updateState.value = UpdateState.Loading
        coroutineScope.launch {
            _updateState.value = runPostProcess()
        }
    }

    /** Преобразует успех или исключение постобработки в явное состояние UI. */
    private suspend fun runPostProcess(): UpdateState = runCatching {
        postProcessAfterUpdate()
    }.fold(
        onSuccess = { UpdateState.Success },
        onFailure = { UpdateState.PostProcessError(it.message ?: "Не удалось выполнить пересчёт") },
    )

    private val _updateState: MutableStateFlow<UpdateState> = MutableStateFlow(UpdateState.Init)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun resetState() {
        _updateState.update { UpdateState.Init }
    }

}

