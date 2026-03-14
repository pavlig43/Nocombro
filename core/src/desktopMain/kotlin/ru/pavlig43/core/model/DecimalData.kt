package ru.pavlig43.core.model

import kotlin.math.pow


//data class DecimalData(
//    val value: Int,
//    val format: DecimalFormat
//) : Number(), Comparable<DecimalData> {
//    override fun compareTo(other: DecimalData): Int {
//        require(format == other.format) {
//            "Cannot compare DecimalData with different formats: $format vs ${other.format}"
//        }
//        return value.compareTo(other.value)
//    }
//
//    operator fun plus(other: DecimalData): DecimalData {
//        require(format == other.format) {
//            "Cannot add DecimalData with different formats: $format vs ${other.format}"
//        }
//        return copy(value = value + other.value)
//    }
//
//    operator fun minus(other: DecimalData): DecimalData {
//        require(format == other.format) {
//            "Cannot subtract DecimalData with different formats: $format vs ${other.format}"
//        }
//        return copy(value = value - other.value)
//    }
//
//    operator fun times(multiplier: Int): DecimalData = copy(value = value * multiplier)
//
//    override fun toDouble(): Double {
//        return value.toDouble()
//    }
//
//    override fun toFloat(): Float {
//        return value.toFloat()
//    }
//
//    override fun toLong(): Long {
//        return value.toLong()
//    }
//
//    override fun toInt(): Int {
//        return value
//    }
//
//    override fun toShort(): Short {
//        return value.toShort()
//    }
//
//    override fun toByte(): Byte {
//        return value.toByte()
//    }
//}
data class DecimalData2(
    override val value: Int
): DecimalData() {
    override val countDecimal: Int = 2

    override fun copyValue(value: Int): DecimalData  = copyValue(value = value)
}
class DecimalData3(
    override val value: Int
): DecimalData() {
    override val countDecimal: Int = 3

    override fun copyValue(value: Int): DecimalData  = copyValue(value = value)
}

abstract class DecimalData: Number(), Comparable<DecimalData> {
    abstract val value: Int
    abstract val countDecimal: Int
    protected abstract fun copyValue(value: Int): DecimalData
    override fun compareTo(other: DecimalData): Int {
        return value.compareTo(other.value)
    }

    operator fun plus(other: DecimalData): DecimalData {
        return copyValue(value = value + other.value)
    }

    operator fun minus(other: DecimalData): DecimalData {

        return copyValue(value = value - other.value)
    }

    operator fun times(multiplier: Int): DecimalData = copyValue(value = value * multiplier)

    override fun toDouble(): Double {
        return value.toDouble()
    }

    override fun toFloat(): Float {
        return value.toFloat()
    }

    override fun toLong(): Long {
        return value.toLong()
    }

    override fun toInt(): Int {
        return value
    }

    override fun toShort(): Short {
        return value.toShort()
    }

    override fun toByte(): Byte {
        return value.toByte()
    }
}
//@Suppress("MagicNumber")
//fun DecimalData.toStartDoubleFormat(): String {
//    return (value / (10.0.pow(format.countDecimal))).toString()
//        .dropLastWhile { it == '0' }
//        .run { if (last() == '.') dropLast(1) else this }
//}
@Suppress("MagicNumber")
fun DecimalData.toStartDoubleFormat(): String {
    return (value / (10.0.pow(countDecimal))).toString()
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

