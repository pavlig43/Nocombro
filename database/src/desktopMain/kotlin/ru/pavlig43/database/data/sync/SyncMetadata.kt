package ru.pavlig43.database.data.sync

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.datetime.getCurrentLocalDateTime
import java.util.UUID

/**
 * Создает глобальный идентификатор записи для синхронизации между устройствами.
 */
fun defaultSyncId(): String = UUID.randomUUID().toString()

/**
 * Возвращает текущее локальное время как время последнего изменения записи.
 */
fun defaultUpdatedAt(): LocalDateTime = getCurrentLocalDateTime()
