package ru.pavlig43.loadinitdata.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository

class LoadInitDataComponent<I : Any>(
    componentContext: ComponentContext,
    private val initDataRepository: IInitDataRepository<I>,
    private val id:Int,
    private val onSuccessGetInitData:(I)->Unit,
) : ComponentContext by componentContext, ILoadInitDataComponent<I> {

    private val coroutineScope = componentCoroutineScope()

    private val _loadState: MutableStateFlow<LoadInitDataState<I>> =
        MutableStateFlow(LoadInitDataState.Loading())

    override val loadState: StateFlow<LoadInitDataState<I>> = _loadState.asStateFlow()

    override fun retryLoadInitData() {
        loadData()
    }

    init {
        loadData()
    }

    private fun loadData() {

        coroutineScope.launch {
            _loadState.update { LoadInitDataState.Loading() }
            val initData = initDataRepository.loadInitData(id).toLoadInitDataSate()
            if (initData is LoadInitDataState.Success){
                onSuccessGetInitData(initData.data)
            }
            _loadState.update { initData }
        }

    }
}

private fun <I : Any> RequestResult<I>.toLoadInitDataSate(): LoadInitDataState<I> {
    return when (this) {
        is RequestResult.Error<I> -> LoadInitDataState.Error(message ?: "Неизвестная ошибка")
        is RequestResult.InProgress -> LoadInitDataState.Loading()
        is RequestResult.Initial<I> -> LoadInitDataState.Loading()
        is RequestResult.Success<I> -> LoadInitDataState.Success(data)
    }
}