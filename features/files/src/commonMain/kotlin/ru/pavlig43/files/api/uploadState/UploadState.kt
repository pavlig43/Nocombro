package ru.pavlig43.files.api.uploadState

sealed interface UploadState {

    data object Loading : UploadState
    data object Success : UploadState

    data class Error(val message: String) : UploadState
}