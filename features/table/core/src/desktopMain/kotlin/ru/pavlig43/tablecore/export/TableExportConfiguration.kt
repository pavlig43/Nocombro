package ru.pavlig43.tablecore.export

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.ItemType

/**
 * Единый тип значения для экспорта.
 *
 * UI-таблица может показывать что угодно, но в exporter мы хотим
 * уже нормализованные типы: текст, число, дата, дата-время и т.д.
 */
sealed interface ExportCellValue {
    data class Text(val value: String) : ExportCellValue
    data class Number(val value: Double) : ExportCellValue
    data class Date(val value: LocalDate) : ExportCellValue
    data class DateTime(val value: LocalDateTime) : ExportCellValue
    data class BooleanValue(val value: Boolean) : ExportCellValue
    data object Empty : ExportCellValue
}

/**
 * Форматы, которые умеет показывать UI-меню экспорта.
 *
 * Сейчас реально реализован только Excel, но UI и конфиг уже готовы к расширению.
 */
enum class TableExportFormat {
    Excel,
    Pdf,
    Word,
}

/**
 * Конфигурация экспорта для конкретной таблицы.
 *
 * `suggestedFileName` задает имя файла по умолчанию.
 * `supportedFormats` управляет доступными пунктами в UI.
 * `formatCellValue` позволяет переопределить сериализацию отдельных колонок.
 */
data class TableExportConfiguration<T : Any, C>(
    val suggestedFileName: String,
    val supportedFormats: Set<TableExportFormat> = setOf(TableExportFormat.Excel),
    val formatCellValue: (column: C, rawValue: Any?, item: T) -> ExportCellValue? = { _, _, _ -> null },
)

/**
 * Возвращает типизированное значение для export-ячейки.
 *
 * Сначала даем таблице шанс переопределить формат конкретной ячейки,
 * а если специального правила нет, падаем обратно на стандартное преобразование.
 */
fun <T : Any, C> TableExportConfiguration<T, C>.formatValue(
    column: C,
    rawValue: Any?,
    item: T,
): ExportCellValue = formatCellValue(column, rawValue, item) ?: defaultExportValue(rawValue)

/**
 * Базовое преобразование "сырых" значений модели в типизированные export-ячейки.
 *
 * Это покрывает большую часть простых колонок без отдельного кода в feature-модуле.
 */
fun defaultExportValue(rawValue: Any?): ExportCellValue =
    when (rawValue) {
        null -> ExportCellValue.Empty
        is ExportCellValue -> rawValue
        is String -> ExportCellValue.Text(rawValue)
        is ItemType -> ExportCellValue.Text(rawValue.displayName)
        is Int -> ExportCellValue.Number(rawValue.toDouble())
        is Long -> ExportCellValue.Number(rawValue.toDouble())
        is Float -> ExportCellValue.Number(rawValue.toDouble())
        is Double -> ExportCellValue.Number(rawValue)
        is Short -> ExportCellValue.Number(rawValue.toDouble())
        is Byte -> ExportCellValue.Number(rawValue.toDouble())
        is Number -> ExportCellValue.Number(rawValue.toDouble())
        is LocalDate -> ExportCellValue.Date(rawValue)
        is LocalDateTime -> ExportCellValue.DateTime(rawValue)
        is Boolean -> ExportCellValue.BooleanValue(rawValue)
        else -> ExportCellValue.Text(rawValue.toString())
    }
