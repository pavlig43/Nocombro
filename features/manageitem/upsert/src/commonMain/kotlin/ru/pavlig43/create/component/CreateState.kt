package ru.pavlig43.create.component

internal interface CreateState {
    data object Init : CreateState
    data object Loading : CreateState
    data class Success(val id: Int) : CreateState
    data class Error(val message: String) : CreateState
}