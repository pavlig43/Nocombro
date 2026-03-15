package ru.pavlig43.core.model

import kotlin.math.pow



data class DecimalData2(
    override val value: Int
): DecimalData() {
    override val countDecimal: Int = 2

    override fun copyValue(value: Int): DecimalData = copy(value = value)

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
    override val value: Int
): DecimalData() {
    @Suppress("MagicNumber")
    override val countDecimal: Int = 3

    override fun copyValue(value: Int): DecimalData = copy(value = value)

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
    abstract val value: Int
    abstract val countDecimal: Int
    abstract fun copyValue(value: Int): DecimalData
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



    override fun hashCode(): Int {
        var result = value
        result = 31 * result + countDecimal
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DecimalData
        return value == other.value && countDecimal == other.countDecimal
    }
}

@Suppress("MagicNumber")
fun DecimalData.toStartDoubleFormat(): String {
    return (value / (10.0.pow(countDecimal))).toString()
        .dropLastWhile { it == '0' }
        .run { if (last() == '.') dropLast(1) else this }
}



