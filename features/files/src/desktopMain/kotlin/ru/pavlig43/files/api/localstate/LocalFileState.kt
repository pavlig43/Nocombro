package ru.pavlig43.files.api.localstate

sealed interface LocalFileState {

    data object Saving : LocalFileState
    data object Saved : LocalFileState

    data class Error(val message: String) : LocalFileState
}
