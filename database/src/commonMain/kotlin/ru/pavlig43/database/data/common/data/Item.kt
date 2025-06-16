package ru.pavlig43.database.data.common.data

import ru.pavlig43.core.UTC

interface Item {
    val id:Int
    val displayName:String
    val type: ItemType
    val createdAt:UTC

}