package ru.pavlig43.core

sealed class RequestResult<out E>(open val data: E? = null) {
    class Initial<E> : RequestResult<E>()
    class InProgress : RequestResult<Nothing>()
    class Success<E>(override val data: E) : RequestResult<E>(data)
    class Error<E>(
        data: E? = null,
        val throwable: Throwable? = null,
        val message: String? = null
    ) : RequestResult<E>(data)
}
fun <I, O> RequestResult<I>.mapTo(mapper: (I) -> O): RequestResult<O> {
    return when (this) {
        is RequestResult.Success -> RequestResult.Success(data = mapper(data))
        is RequestResult.Error -> RequestResult.Error(data = data?.let(mapper),throwable = throwable,message = message)
        is RequestResult.InProgress -> RequestResult.InProgress()
        is RequestResult.Initial -> RequestResult.Initial()
    }
}


