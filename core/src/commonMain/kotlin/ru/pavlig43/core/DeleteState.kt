package ru.pavlig43.core


sealed interface DeleteState{
    class Initial : DeleteState
    class Loading : DeleteState
    class Success(val message: String) : DeleteState
    class Error(val message: String) : DeleteState
}
fun RequestResult<Unit>.toDeleteState(): DeleteState {
    return when (this) {
        is RequestResult.Error<*> -> DeleteState.Error(message ?: "unknown error")
        is RequestResult.InProgress -> DeleteState.Loading()
        is RequestResult.Initial<*> -> DeleteState.Initial()
        is RequestResult.Success<*> -> DeleteState.Success("success")
    }
}