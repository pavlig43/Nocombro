package ru.pavlig43.loadinitdata.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.componentCoroutineScope

/**
 * Компонент для загрузки и управления начальными данными.
 *
 * Этот компонент отвечает за:
 * 1. Асинхронную загрузку начальных данных при инициализации
 * 2. Хранение оригинальных (первых) загруженных данных для сравнения с изменениями пользователя
 * 3. Обработку состояний загрузки (загрузка, успех, ошибка)
 * 4. Предоставление возможности повторной загрузки при ошибках
 *
 * @param getInitData Suspend-функция для загрузки начальных данных. Возвращает [Result] с данными или ошибкой
 * @param onSuccessGetInitData Callback, вызываемый при успешной загрузке данных для их дальнейшей обработки
 * передает наверх данные, если они успешно загрузились
 *
 * @property firstData [StateFlow] с оригинальными загруженными данными. Используется для сравнения
 *                     с изменёнными пользователем данными перед сохранением в БД
 * @property loadState [StateFlow] с текущим состоянием загрузки данных
 *
 * ## Жизненный цикл:
 * 1. **Инициализация**: Автоматически начинает загрузку данных в `init` блоке
 * 2. **Загрузка**: Вызывает `getInitData()` в IO-диспетчере
 * 3. **Обработка результата**:
 *    - При успехе: сохраняет данные в `firstData`, вызывает `onSuccessGetInitData`
 *    - При ошибке: сохраняет состояние ошибки в `loadState`
 * 4. **Повторная загрузка**: Через `retryLoadInitData()` можно повторить загрузку

 */
class LoadInitDataComponent<I : Any>(
    componentContext: ComponentContext,
    private val getInitData: suspend () -> Result<I>,
    private val onSuccessGetInitData: (I) -> Unit,
) : ComponentContext by componentContext {

    private val coroutineScope = componentCoroutineScope()

    /**
     * Первые данные которые загрузились.
     * Нужны, для того чтобы сравнивать первичные данные и те,
     * которые пользователь изменил для записи в бд
     */
    private val _firstData = MutableStateFlow<I?>(null)
    val firstData = _firstData.asStateFlow()



    private val _loadState: MutableStateFlow<LoadInitDataState<I>> =
        MutableStateFlow(LoadInitDataState.Loading())

    val loadState: StateFlow<LoadInitDataState<I>> = _loadState.asStateFlow()

    fun retryLoadInitData() {
        loadData()
    }

    init {
        loadData()
    }

    private fun loadData() {

        coroutineScope.launch(Dispatchers.IO) {

            _loadState.update { LoadInitDataState.Loading() }
            val initData =  getInitData().fold(
                onSuccess = { LoadInitDataState.Success(it) },
                onFailure ={ LoadInitDataState.Error(it.message ?: "Неизвестная ошибка")}
            )
            if (initData is LoadInitDataState.Success){
                _firstData.update { initData.data }
                onSuccessGetInitData(initData.data)
            }
            _loadState.update { initData }
        }
    }
}


sealed interface LoadInitDataState<I> {
    class Loading<I> : LoadInitDataState<I>
    class Error<I>(val message: String) : LoadInitDataState<I>
    class Success<I>(val data:I) : LoadInitDataState<I>
}