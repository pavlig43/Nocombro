package ru.pavlig43.product.internal

/**
 * Поля для таблицы создания продукта
 */
internal enum class ProductField {
    DISPLAY_NAME,
    /** Тип продукта */
    PRODUCT_TYPE,
    /** Рекомендованная цена продажи */
    PRICE_FOR_SALE,
    /** Дата создания */
    CREATED_AT,
    /** Комментарий */
    COMMENT,
}
