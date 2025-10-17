package ru.pavlig43.declarationlist.internal.data

internal sealed interface DeclarationListState {
    class Initial : DeclarationListState
    class Loading : DeclarationListState
    class Success(val data: List<DeclarationItemUi>) : DeclarationListState
    class Error(val message: String) : DeclarationListState
}