package ru.pavlig43.core.model

import java.text.DecimalFormat
import kotlin.math.pow



data class DecimalData2(
    override val value: Long
): DecimalData() {
    override val countDecimal: Int = 2

    override fun copyValue(value: Long): DecimalData = copy(value = value)

    override fun toString(): String = toStartDoubleFormat()

    operator fun plus(other: DecimalData2): DecimalData2 {
        require(other.countDecimal == countDecimal) { "Cannot add DecimalData with different decimal places" }
        return copy(value = value + other.value)
    }
    operator fun minus(other: DecimalData2): DecimalData2 {
        require(other.countDecimal == countDecimal) { "Cannot subtract DecimalData with different decimal places" }
        return copy(value = value - other.value)
    }

    operator fun times(multiplier: Int): DecimalData2 = copy(value = value * multiplier)
}
data class DecimalData3(
    override val value: Long
): DecimalData() {
    @Suppress("MagicNumber")
    override val countDecimal: Int = 3

    override fun copyValue(value: Long): DecimalData = copy(value = value)

    override fun toString(): String = toStartDoubleFormat()

    operator fun plus(other: DecimalData3): DecimalData3 {
        require(other.countDecimal == countDecimal) { "Cannot add DecimalData with different decimal places" }
        return copy(value = value + other.value)
    }
    operator fun minus(other: DecimalData3): DecimalData3 {
        require(other.countDecimal == countDecimal) { "Cannot subtract DecimalData with different decimal places" }
        return copy(value = value - other.value)
    }
    operator fun times(multiplier: Int): DecimalData3 = copy(value = value * multiplier)
}

abstract class DecimalData: Number(), Comparable<DecimalData> {
    abstract val value: Long
    abstract val countDecimal: Int
    abstract fun copyValue(value: Long): DecimalData
    override fun compareTo(other: DecimalData): Int {
        return value.compareTo(other.value)
    }

    
    override fun toDouble(): Double {
        return value.toDouble()
    }

    override fun toFloat(): Float {
        return value.toFloat()
    }

    override fun toLong(): Long {
        return value
    }

    override fun toInt(): Int {
        return value.toInt()
    }

    override fun toShort(): Short {
        return value.toShort()
    }

    override fun toByte(): Byte {
        return value.toByte()
    }



    override fun hashCode(): Int = value.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DecimalData
        return value == other.value && countDecimal == other.countDecimal
    }
}

private val formatter = DecimalFormat("#,##0.###")

@Suppress("MagicNumber")
fun DecimalData.toStartDoubleFormat(): String {
    val doubleValue = value / (10.0.pow(countDecimal))
    return formatter.format(doubleValue)
}
inline fun <T> Iterable<T>.sumOfDecimal2(selector: (T) -> DecimalData2): DecimalData2 {
    var sum: DecimalData2 = DecimalData2(0)
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
inline fun <T> Iterable<T>.sumOfDecimal3(selector: (T) -> DecimalData3): DecimalData3 {
    var sum: DecimalData3 = DecimalData3(0)
    for (element in this) {
        sum += selector(element)
    }
    return sum
}



