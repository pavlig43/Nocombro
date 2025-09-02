package ru.pavlig43.loadinitdata.api.component

import kotlinx.coroutines.flow.StateFlow

interface ILoadInitDataComponent<I : Any> {

    val firstData:StateFlow<I?>

    val loadState: StateFlow<LoadInitDataState<I>>

    fun retryLoadInitData()
}

sealed interface LoadInitDataState<I : Any> {
    class Loading<I : Any> : LoadInitDataState<I>
    class Error<I : Any>(val message: String) : LoadInitDataState<I>
    class Success<I : Any>(val data:I) : LoadInitDataState<I>
}