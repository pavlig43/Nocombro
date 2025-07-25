package ru.pavlig43.itemlist.api.data

import ru.pavlig43.database.data.common.data.ItemType

interface ItemUi {
    val id:Int
    val displayName:String
    val type: ItemType
    val createdAt:String
}
