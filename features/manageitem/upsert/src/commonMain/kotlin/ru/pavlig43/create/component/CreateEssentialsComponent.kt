package ru.pavlig43.create.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.component.EssentialsComponent
import ru.pavlig43.core.model.GenericItem
import ru.pavlig43.core.model.ItemEssentialsUi
import ru.pavlig43.create.data.CreateEssentialsRepository

/**
 * Абстрактный компонент для создания новых сущностей.
 *
 * Расширяет [EssentialsComponent] добавляя функциональность создания новых записей
 * с отправкой данных в репозиторий и обработкой результата операции.
 *
 * @param I Тип предметной модели (GenericItem) - DTO для отправки в бэкенд/БД
 * @param T Тип UI-модели (ItemEssentialsUi) - данные формы
 * @param onSuccessCreate Callback вызываемый при успешном создании сущности.
 *                        Принимает ID созданной записи для дальнейшей навигации/обработки
 * @param componentFactory Фабрика с логикой преобразования и валидации данных
 * @param createEssentialsRepository Репозиторий для создания сущностей в системе
 * @param mapperToDTO Функция преобразования UI-модели (T) в предметную модель (I)
 *                    для отправки на сервер/сохранения в БД
 *
 *
 * ## Поток создания сущности:
 * 1. **Заполнение формы**: Пользователь заполняет поля в UI
 * 2. **Валидация**: Поля проверяются через `isValidValuesFactory` из фабрики
 * 3. **Создание**: При вызове `create()`:
 *    - Данные преобразуются из UI-модели в DTO через `mapperToDTO`
 *    - Отправляются в репозиторий `createEssentialsRepository`
 *    - Обновляется `createState` для отображения прогресса/результата
 * 4. **Обработка результата**:
 *    - При успехе: вызывается `onSuccessCreate` с ID новой записи
 *    - При ошибке: состояние ошибки доступно в UI для показа пользователю
 * @see EssentialsComponent Базовый компонент для работы с формами
 */
abstract class CreateEssentialsComponent<I : GenericItem, T : ItemEssentialsUi>(
    componentContext: ComponentContext,
    val onSuccessCreate: (Int) -> Unit,
    componentFactory: EssentialComponentFactory<I, T>,
    private val createEssentialsRepository: CreateEssentialsRepository<I>,
    private val mapperToDTO: T.() -> I,
) : EssentialsComponent<I, T>(
    componentContext = componentContext,
    componentFactory = componentFactory,
    getInitData = null,

    ) {

    private val _createState: MutableStateFlow<CreateState> = MutableStateFlow(CreateState.Init)
    internal val createState = _createState.asStateFlow()

    internal fun create() {
        coroutineScope.launch(Dispatchers.IO) {
            _createState.update { CreateState.Loading }
            val item = itemFields.value.mapperToDTO()
            val idResult = createEssentialsRepository.createEssential(item)
            val state = idResult.fold(
                onSuccess = { CreateState.Success(it) },
                onFailure = { CreateState.Error(it.message ?: "Неизвестная ошибка") }
            )
            _createState.update { state }
        }
    }

}