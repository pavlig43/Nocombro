package ru.pavlig43.core.data

interface Item:GenericItem {
    override val id:Int
    override val displayName:String
    val type: ItemType
    val createdAt:Long
    val comment:String
}
interface GenericItem{
    val id:Int
    val displayName:String
}
