package ru.pavlig43.itemlist.api.data

import ru.pavlig43.core.data.ItemType
import ru.pavlig43.coreui.itemlist.IItemUi


data class ItemUi(
    override val id:Int,
    override val displayName:String,
    val type: ItemType,
    val createdAt:String,
    val comment:String = ""
):IItemUi

