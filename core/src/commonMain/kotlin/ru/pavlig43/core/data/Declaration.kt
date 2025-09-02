package ru.pavlig43.core.data


interface DeclarationOut:CollectionObject{
    override val id: Int
    val parentId:Int
    val documentId:Int
    val isActual: Boolean
    val displayName: String
}
interface DeclarationIn:CollectionObject{
    override val id:Int
    val parentId:Int
    val documentId:Int
    val isActual: Boolean
}
