package ru.pavlig43.immutable.internal.column

import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.toStartDoubleFormat
import ru.pavlig43.datetime.dateFormat
import ru.pavlig43.datetime.dateTimeFormat

internal val tableCellHorizontalPadding = 12.dp

internal fun LocalDate.toTableDisplayText(): String = format(dateFormat)

internal fun LocalDateTime.toTableDisplayText(): String = format(dateTimeFormat)

internal fun DecimalData.toTableDisplayText(): String = toStartDoubleFormat()
