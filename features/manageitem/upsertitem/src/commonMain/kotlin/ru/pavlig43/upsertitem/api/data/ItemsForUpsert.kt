package ru.pavlig43.upsertitem.api.data

import ru.pavlig43.core.data.Item


/**
 * Получаем item,который хотим сохранить или обновить и item,который изначальный для сохранения
 */
data class ItemsForUpsert<I: Item>(
    val newItem: I,
    val initItem: I
)