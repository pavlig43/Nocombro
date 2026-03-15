package ru.pavlig43.immutable.internal.component.items.batch

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class BatchTableUi(
    override val composeId: Int,
    val batchId: Int,
    val balance: DecimalData3,
    val productName: String,
    val vendorName: String,
    val dateBorn: LocalDate
): IMultiLineTableUi
