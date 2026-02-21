package ru.pavlig43.immutable.internal.component.items.batch

import kotlinx.datetime.LocalDate
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class BatchTableUi(
    override val composeId: Int,
    val batchId: Int,
    val count: Int,
    val vendorName: String,
    val dateBorn: LocalDate
): IMultiLineTableUi
