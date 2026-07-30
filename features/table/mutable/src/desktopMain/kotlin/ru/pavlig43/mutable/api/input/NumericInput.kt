package ru.pavlig43.mutable.api.input

import ru.pavlig43.core.model.DecimalData

/** Текст и точное целое значение дробного поля после чистки ввода. */
internal data class DecimalInput(
    val text: String,
    val value: Long,
)

/** Разбирает целое число и отклоняет текст вне [range]. */
internal fun String.toIntInputValueOrNull(range: IntRange): Int? {
    if (isEmpty()) return 0.takeIf { it in range }
    if (any { !it.isDigit() }) return null
    return toIntOrNull()?.takeIf { it in range }
}

/**
 * Чистит и разбирает дробный ввод по старым правилам таблиц.
 * Первый нецифровой знак становится точкой, следующие отбрасываются.
 */
internal fun String.toDecimalInputOrNull(
    decimalPlaces: Int,
    range: LongRange = 0L..Long.MAX_VALUE,
): DecimalInput? {
    val text = normalizeDecimalInput(decimalPlaces)
    val value = text.toDecimalInputValueOrNull(decimalPlaces, range) ?: return null
    return DecimalInput(text = text, value = value)
}

/** Даёт дробный текст без незначащих нулей справа. */
internal fun DecimalData.toDecimalInputText(): String {
    if (value == 0L) return ""

    val scale = decimalScale(countDecimal) ?: return value.toString()
    val whole = value / scale
    if (countDecimal == 0) return whole.toString()

    val fraction = (value % scale)
        .toString()
        .padStart(countDecimal, '0')
        .trimEnd('0')
    return if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
}

/** Оставляет цифры, одну точку и допустимое число знаков после неё. */
internal fun String.normalizeDecimalInput(decimalPlaces: Int): String {
    require(decimalPlaces >= 0) { "decimalPlaces must not be negative" }

    val normalized = fold("") { result, char ->
        when {
            char.isDigit() -> result + char
            '.' !in result -> "$result."
            else -> result
        }
    }
    val parts = normalized.split('.')
    return if (parts.size == 2) {
        parts[0] + "." + parts[1].take(decimalPlaces)
    } else {
        normalized
    }
}

/** Разбирает уже очищенный дробный текст без потери точности. */
private fun String.toDecimalInputValueOrNull(
    decimalPlaces: Int,
    range: LongRange,
): Long? {
    if (isEmpty()) return 0L.takeIf { it in range }
    if (this == ".") return null

    val parts = split('.', limit = 2)
    val fractionText = parts.getOrNull(1).orEmpty()
    val whole = parts.first().ifEmpty { "0" }.toLongOrNull() ?: return null
    val scale = decimalScale(decimalPlaces) ?: return null
    val fraction = fractionText
        .padEnd(decimalPlaces, '0')
        .ifEmpty { "0" }
        .toLongOrNull()
        ?: return null

    if (whole > (Long.MAX_VALUE - fraction) / scale) return null
    return (whole * scale + fraction).takeIf { it in range }
}

/** Возвращает множитель для числа знаков или `null` при переполнении. */
private fun decimalScale(decimalPlaces: Int): Long? {
    var scale = 1L
    repeat(decimalPlaces) {
        if (scale > Long.MAX_VALUE / 10L) return null
        scale *= 10L
    }
    return scale
}
