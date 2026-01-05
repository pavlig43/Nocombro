package ru.pavlig43.immutable.internal.component.items.declaration

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.tablecore.model.ITableUi


data class DeclarationTableUi(

    val displayName: String="",

    val createdAt: LocalDate,

    val vendorId: Int = 0,

    val vendorName: String = "",

    val bestBefore: LocalDate,

    override val composeId: Int = 0,
): ITableUi{
    val isActual: Boolean = bestBefore > getCurrentLocalDate()
}