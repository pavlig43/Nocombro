package ru.pavlig43.mutable.api.singleLine.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository

/**
 * Управляет созданием одной записи из карточной формы.
 *
 * @param onSuccessCreate вызывается с ID созданной записи
 * @param componentFactory задаёт начальную UI-модель, проверку и перевод данных
 * @param createSingleItemRepository сохраняет новую запись
 * @param mapperToDTO переводит UI-модель в предметную модель
 */
abstract class CreateSingleLineComponent<I : SingleItem, UI : Any>(
    componentContext: ComponentContext,
    val onSuccessCreate: (Int) -> Unit,
    componentFactory: SingleLineComponentFactory<I, UI>,
    private val createSingleItemRepository: CreateSingleItemRepository<I>,
    observeOnItem: (UI) -> Unit,
    private val mapperToDTO: UI.() -> I,
) : SingleLineComponent<I, UI>(
    componentContext = componentContext,
    componentFactory = componentFactory,
    observeOnItem = observeOnItem,
    getInitData = null,
) {
    private val _createState: MutableStateFlow<CreateState> = MutableStateFlow(CreateState.Init)
    internal val createState = _createState.asStateFlow()

    /** Создаёт запись и публикует ход запроса в [createState]. */
    fun create() {
        coroutineScope.launch(Dispatchers.IO) {
            _createState.update { CreateState.Loading }
            val item = item.value.mapperToDTO()
            val idResult = createSingleItemRepository.createEssential(item)
            val state = idResult.fold(
                onSuccess = { CreateState.Success(it) },
                onFailure = { CreateState.Error(it.message ?: "Неизвестная ошибка") },
            )
            _createState.update { state }
        }
    }
}

/** Состояние создания записи. */
internal sealed interface CreateState {
    /** Начальное состояние. */
    data object Init : CreateState

    /** Идёт создание. */
    data object Loading : CreateState

    /** Запись создана. */
    data class Success(val id: Int) : CreateState

    /** Создать запись не удалось. */
    data class Error(val message: String) : CreateState
}
