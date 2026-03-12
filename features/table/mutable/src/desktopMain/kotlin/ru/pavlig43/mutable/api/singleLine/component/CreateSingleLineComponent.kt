package ru.pavlig43.mutable.api.singleLine.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

/**
 * Абстрактный компонент для создания новых сущностей через таблицу с одной строкой.
 *
 * Расширяет [SingleLineComponent] добавляя функциональность создания новых записей
 * с отправкой данных в репозиторий и обработкой результата операции.
 *
 * @param I Тип предметной модели (GenericItem) - DTO для отправки в бэкенд/БД
 * @param UI Тип UI-модели (ITableUi) - данные формы
 * @param onSuccessCreate Callback вызываемый при успешном создании сущности.
 *                        Принимает ID созданной записи для дальнейшей навигации/обработки
 * @param componentFactory Фабрика с логикой преобразования и валидации данных
 * @param createSingleItemRepository Репозиторий для создания сущностей в системе
 * @param mapperToDTO Функция преобразования UI-модели (UI) в предметную модель (I)
 *                    для отправки на сервер/сохранения в БД
 *
 * ## Поток создания сущности:
 * 1. **Заполнение формы**: Пользователь заполняет поля в таблице с одной строкой
 * 2. **Валидация**: Поля проверяются через `isValidFieldsFactory` из фабрики
 * 3. **Создание**: При вызове `create()`:
 *    - Данные преобразуются из UI-модели в DTO через `mapperToDTO`
 *    - Отправляются в репозиторий `createSingleItemRepository`
 *    - Обновляется `createState` для отображения прогресса/результата
 * 4. **Обработка результата**:
 *    - При успехе: вызывается `onSuccessCreate` с ID новой записи
 *    - При ошибке: состояние ошибки доступно в UI для показа пользователю
 * @see SingleLineComponent Базовый компонент для работы с формами
 */
abstract class CreateSingleLineComponent<I : SingleItem, UI : ISingleLineTableUi, C>(
    componentContext: ComponentContext,
    val onSuccessCreate: (Int) -> Unit,
    componentFactory: SingleLineComponentFactory<I, UI>,
    private val createSingleItemRepository: CreateSingleItemRepository<I>,
    observeOnItem:(UI)-> Unit,
    private val mapperToDTO: UI.() -> I,
) : SingleLineComponent<I, UI, C>(
    componentContext = componentContext,
    componentFactory = componentFactory,
    observeOnItem = observeOnItem,
    getInitData = null,
) {
    private val _createState: MutableStateFlow<CreateState> = MutableStateFlow(CreateState.Init)
    internal val createState = _createState.asStateFlow()

    fun create() {
        coroutineScope.launch(Dispatchers.IO) {
            _createState.update { CreateState.Loading }
            val item = item.value.mapperToDTO()
            val idResult = createSingleItemRepository.createEssential(item)
            val state = idResult.fold(
                onSuccess = { CreateState.Success(it) },
                onFailure = { CreateState.Error(it.message ?: "Неизвестная ошибка") }
            )
            _createState.update { state }
        }
    }
}

/**
 * Состояние процесса создания сущности
 */
internal sealed interface CreateState {
    /** Начальное состояние */
    data object Init : CreateState

    /** Идет создание */
    data object Loading : CreateState

    /** Успешное создание */
    data class Success(val id: Int) : CreateState

    /** Ошибка при создании */
    data class Error(val message: String) : CreateState
}
