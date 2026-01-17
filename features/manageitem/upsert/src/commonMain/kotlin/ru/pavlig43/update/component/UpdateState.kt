package ru.pavlig43.update.component

sealed interface UpdateState {
    data object Init: UpdateState
    data object Loading : UpdateState
    data object Success : UpdateState
    data class Error(val message: String) : UpdateState
}