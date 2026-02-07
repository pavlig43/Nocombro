package ru.pavlig43.core.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.model.GenericItem
import ru.pavlig43.core.model.ItemEssentialsUi
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent


/**
 * Фабрика для создания компонентов работы с основными данными сущностей.
 *
 * @param I Тип предметной области (GenericItem) - модель данных из бэкенда/БД,
 * не особо нужна, сделал для типизации
 * @param T Тип UI-модели (ItemEssentialsUi) - представление данных для отображения
 * @property initItem Начальное (пустое) состояние UI-модели для инициализации формы
 * @property isValidFieldsFactory Функция-валидатор, проверяющая корректность заполнения полей формы.
 *  Возвращает `true` если все обязательные поля заполнены корректно
 * @property mapperToUi Функция преобразования предметной модели (I) в UI-модель (T) для отображения
 * @property produceInfoForTabName Callback для обновления информации в названии вкладки(та которая с крестиком).
 */
data class EssentialComponentFactory<I : GenericItem, T : ItemEssentialsUi>(
    val initItem: T,
    val isValidFieldsFactory: T.() -> Boolean,
    val mapperToUi: I.() -> T,
    val produceInfoForTabName: (T) -> Unit,
)
/**
 * Абстрактный компонент для работы с основными полями сущности.
 * Используется как при создании, так и при обновлении.
 *
 * Предоставляет общую логику для:
 * 1. Загрузки и отображения данных сущности
 * 2. Валидации полей формы
 * 3. Преобразования между предметной и UI-моделями
 * 4. Управления состоянием формы
 *
 * @param I Тип предметной модели (GenericItem)
 * @param T Тип UI-модели (ItemEssentialsUi)
 * @param componentFactory Фабрика с логикой преобразования и валидации
 * @param getInitData Опциональная suspend-функция для загрузки начальных данных.
 *                    Если `null`, используется [initItem] из [componentFactory]
 *
 * @property itemFields [kotlinx.coroutines.flow.StateFlow] с текущим состоянием полей формы
 * @property initDataComponent Компонент загрузки начальных данных
 * @property isValidFields  с результатом валидации формы (`true` если все поля корректны)
 * (Нужна, чтобы передавать потом в Button enabled)
 *
 * ## Поток данных:
 * 1. **Инициализация**: Создаётся `LoadInitDataComponent` для загрузки данных
 * 2. **Загрузка**: Если `getInitData` предоставлен, данные загружаются и преобразуются в UI-модель
 * 3. **Отображение**: Данные сохраняются в `itemFields` и отображаются в UI
 * 4. **Валидация**: При каждом изменении `itemFields` вызывается `isValidValuesFactory`
 * 5. **Обновление**: `onChangeItem()` обновляет состояние и синхронизирует связанный UI
 *
 * @see LoadInitDataComponent Компонент загрузки данных с обработкой состояний
 */
abstract class EssentialsComponent<I : GenericItem, T : ItemEssentialsUi>(
    componentContext: ComponentContext,
    private val componentFactory: EssentialComponentFactory<I, T>,
    getInitData: (suspend () -> Result<I>)?,
    private val observeOnEssentials:(T)-> Unit ={},
    onSuccessInitData:(T)-> Unit = {}
) : ComponentContext by componentContext {
    protected val coroutineScope = componentCoroutineScope()

    private val _itemFields = MutableStateFlow(componentFactory.initItem)
    val itemFields = _itemFields.asStateFlow()

    val initDataComponent = LoadInitDataComponent<T>(
        componentContext = childContext("init"),
        getInitData = {
            getInitData?.invoke()?.map { item ->
                componentFactory.mapperToUi(item)
                    .also { componentFactory.produceInfoForTabName(it) }
            }?:Result.success(componentFactory.initItem)

        },
        onSuccessGetInitData = { item ->
            onSuccessInitData(item)
            _itemFields.update { item }
        }
    )


    fun onChangeItem(item: T) {
        componentFactory.produceInfoForTabName(item)
        _itemFields.update { item }
        observeOnEssentials(item)

    }

    val isValidFields = _itemFields.map { item ->
        componentFactory.isValidFieldsFactory(item)
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        false
    )

}



