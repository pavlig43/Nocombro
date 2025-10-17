package ru.pavlig43.declaration.api.data

data class ProductDeclarationUi(
    val id: Int,
    val composeKey: Int,
    val declarationId: Int,
    val declarationName: String,
    val vendorName: String,
    val isActual: Boolean,
    val bestBefore:Long
)