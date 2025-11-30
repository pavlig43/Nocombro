package ru.pavlig43.core.data

interface UpsertState<out I: GenericItem> {
    object Init : UpsertState<Nothing>
    object Loading : UpsertState<Nothing>
    data class Success<I : GenericItem>(val id: Int) : UpsertState<I>
    data class Error(val message: String) : UpsertState<Nothing>
}