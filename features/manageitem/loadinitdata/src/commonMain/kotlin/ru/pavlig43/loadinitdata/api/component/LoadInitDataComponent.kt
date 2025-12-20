package ru.pavlig43.loadinitdata.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen

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

        coroutineScope.launch {

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