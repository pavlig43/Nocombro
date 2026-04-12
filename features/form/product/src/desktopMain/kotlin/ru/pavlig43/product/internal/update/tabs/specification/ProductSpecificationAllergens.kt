package ru.pavlig43.product.internal.update.tabs.specification

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val allergensJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
}

/**
 * Строка таблицы аллергенов.
 */
@Serializable
internal data class ProductSpecificationAllergenRow(
    val name: String,
    val inProduct: String,
    val inProduction: String,
)

/**
 * Кодирует список аллергенов в JSON для хранения в БД.
 */
internal fun encodeAllergensToJson(
    rows: List<ProductSpecificationAllergenRow>,
): String {
    if (rows.isEmpty()) return ""
    return allergensJson.encodeToString(rows)
}

/**
 * Декодирует аллергены из JSON-строки БД.
 */
internal fun decodeAllergensJson(
    raw: String,
): List<ProductSpecificationAllergenRow> {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return emptyList()

    return runCatching {
        allergensJson.decodeFromString<List<ProductSpecificationAllergenRow>>(trimmed)
    }.getOrDefault(emptyList())
}

/**
 * Преобразует stored-значение в удобный текст для текущего редактора вкладки.
 */
internal fun allergensToEditorText(
    raw: String,
): String {
    return decodeAllergensJson(raw).joinToString("\n") { row ->
        "${row.name}; ${row.inProduct}; ${row.inProduction}"
    }
}

/**
 * Собирает JSON-значение для БД из текста редактора.
 *
 * Формат строки редактора:
 * `Аллерген; в продукте; на производстве`
 */
internal fun editorTextToAllergensJson(
    text: String,
): String {
    val rows = parseEditorLines(text)
    return encodeAllergensToJson(rows)
}

/**
 * Возвращает список ошибок пользовательского ввода аллергенов.
 */
internal fun validateAllergensEditorText(
    text: String,
): List<String> {
    return text.lineSequence()
        .mapIndexed { index, rawLine -> index + 1 to rawLine.trim() }
        .filter { (_, line) -> line.isNotBlank() }
        .mapNotNull { (lineNumber, line) ->
            val cells = line.split(';').map(String::trim)
            when {
                cells.size != 3 -> {
                    "Аллергены: строка $lineNumber должна быть в формате `название; да/нет; да/нет`"
                }
                cells[0].isBlank() -> {
                    "Аллергены: строка $lineNumber должна содержать название аллергена"
                }
                !cells[1].isYesNo() -> {
                    "Аллергены: строка $lineNumber, колонка 'в продукте' должна быть `да` или `нет`"
                }
                !cells[2].isYesNo() -> {
                    "Аллергены: строка $lineNumber, колонка 'на производстве' должна быть `да` или `нет`"
                }
                else -> null
            }
        }
        .toList()
}

private fun parseEditorLines(
    text: String,
): List<ProductSpecificationAllergenRow> {
    return text.lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .mapNotNull { line ->
            val cells = line.split(';').map(String::trim)
            if (cells.size < 3) return@mapNotNull null
            ProductSpecificationAllergenRow(
                name = cells[0],
                inProduct = cells[1],
                inProduction = cells[2],
            )
        }
        .toList()
}

private fun String.isYesNo(): Boolean {
    return equals("да", ignoreCase = true) || equals("нет", ignoreCase = true)
}
