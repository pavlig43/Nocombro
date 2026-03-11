package ru.pavlig43.core.model

import kotlin.math.pow

data class DecimalData(
    val value: Int,
    val format: DecimalFormat
) : Comparable<DecimalData> {
    override fun compareTo(other: DecimalData): Int {
        require(format == other.format) {
            "Cannot compare DecimalData with different formats: $format vs ${other.format}"
        }
        return value.compareTo(other.value)
    }

    operator fun plus(other: DecimalData): DecimalData {
        require(format == other.format) {
            "Cannot add DecimalData with different formats: $format vs ${other.format}"
        }
        return copy(value = value + other.value)
    }

    operator fun minus(other: DecimalData): DecimalData {
        require(format == other.format) {
            "Cannot subtract DecimalData with different formats: $format vs ${other.format}"
        }
        return copy(value = value - other.value)
    }

    operator fun times(multiplier: Int): DecimalData = copy(value = value * multiplier)
}
@Suppress("MagicNumber")
fun DecimalData.toStartDoubleFormat(): String {
    return (value / (10.0.pow(format.countDecimal))).toString()
        .dropLastWhile { it == '0' }
        .run { if (last() == '.') dropLast(1) else this }
}

sealed interface DecimalFormat {
    val countDecimal: Int

    @Suppress("MagicNumber")
    data object Decimal3 : DecimalFormat {
        override val countDecimal: Int = 3
    }

    @Suppress("MagicNumber")
    data object Decimal2 : DecimalFormat {
        override val countDecimal: Int = 2
    }
}

