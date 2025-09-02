package ru.pavlig43.declaration.api.data

data class DeclarationUi(
    val id: Int,
    val composeKey: Int,
    val documentId: Int,
    val isActual: Boolean,
    val name: String,
)