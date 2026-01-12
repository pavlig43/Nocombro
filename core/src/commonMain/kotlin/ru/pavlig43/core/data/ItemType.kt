package ru.pavlig43.core.data

/**
 * Тип объекта. Например, у документа(ГОСТ, ТУ).
 * У продукта(пищевой, упаковка)
 */

interface ItemType {
    val displayName: String
}