package ru.pavlig43.immutable.internal.column

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.ReadonlyTableColumnsBuilder
import kotlin.math.pow



@Suppress("LongParameterList")
fun <T : Any, C, E> ReadonlyTableColumnsBuilder<T, C, E>.readDecimalColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> Int,
    decimalFormat: DecimalFormat,
    alignment: Alignment = Alignment.Center
) {
    column(column, valueOf = { valueOf(it) }) {
        autoWidth(300.dp)
        header(headerText)
        align(alignment)
        readDecimalCell(format = decimalFormat, getCount = valueOf)
        sortable()
    }
}

private fun <T : Any, C, E> ReadonlyColumnBuilder<T, C, E>.readDecimalCell(
    format: DecimalFormat,
    getCount: (T) -> Int,
) {
    cell { item, _ ->
        Text(
            text = getCount(item).toStartDoubleFormat(format),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Suppress("MagicNumber")
private fun Int.toStartDoubleFormat(decimalFormat: DecimalFormat): String {
    return (this / (10.0.pow(decimalFormat.countDecimal))).toString()
        .dropLastWhile { it == '0' }
        .run { if (last() == '.') dropLast(1) else this }
}
sealed interface DecimalFormat {
    val countDecimal: Int

    @Suppress("MagicNumber")
    class Decimal3 : DecimalFormat {
        override val countDecimal: Int = 3
    }

    @Suppress("MagicNumber")
    class Decimal2 : DecimalFormat {
        override val countDecimal: Int = 2
    }
}
