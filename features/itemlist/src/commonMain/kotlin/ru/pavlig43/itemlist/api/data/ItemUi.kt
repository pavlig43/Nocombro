package ru.pavlig43.itemlist.api.data

import ru.pavlig43.core.data.ItemType


data class ItemUi(
    val id:Int,
    val displayName:String,
    val type: ItemType,
    val createdAt:String ,
    val comment:String = ""
)

