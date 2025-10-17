package ru.pavlig43.declarationlist.internal.data

import ru.pavlig43.coreui.itemlist.IItemUi

data class DeclarationItemUi(

    override val displayName: String,

    val createdAt: Long,

    val vendorId: Int,

    val vendorName: String,

    val bestBefore: Long,

    override val id: Int = 0,
): IItemUi