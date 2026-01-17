package ru.pavlig43.core.model

/**
 * Тип объекта. Например, у документа(ГОСТ, ТУ).
 * У продукта(пищевой, упаковка)
 */

interface ItemType {
    val displayName: String
}