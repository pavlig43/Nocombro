package ru.pavlig43.core.data



interface GenericDeclarationIn:CollectionObject{
        val productId: Int
        val declarationId: Int
        override val id: Int
}
interface GenericDeclarationOut:CollectionObject{
    override val id: Int
    val productId: Int
    val declarationId: Int
    val declarationName: String
    val vendorName: String
    val bestBefore: Long
}