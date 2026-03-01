package ru.pavlig43.coreui

import kotlin.math.pow

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

@Suppress("MagicNumber")
fun Int.toStartDoubleFormat(decimalFormat: DecimalFormat): String {
    return (this / (10.0.pow(decimalFormat.countDecimal))).toString()
        .dropLastWhile { it == '0' }
        .run { if (last() == '.') dropLast(1) else this }
}
