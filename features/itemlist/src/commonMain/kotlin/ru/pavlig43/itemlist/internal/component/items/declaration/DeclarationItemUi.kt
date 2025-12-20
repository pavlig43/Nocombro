package ru.pavlig43.itemlist.internal.component.items.declaration

import kotlinx.datetime.LocalDate
import ru.pavlig43.itemlist.api.model.IItemUi

data class DeclarationItemUi(

    val displayName: String="",

    val createdAt: LocalDate ,

    val vendorId: Int = 0,

    val vendorName: String = "",

    val bestBefore: LocalDate,

    override val id: Int = 0,
): IItemUi