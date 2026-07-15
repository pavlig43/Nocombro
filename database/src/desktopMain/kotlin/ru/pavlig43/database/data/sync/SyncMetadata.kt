package ru.pavlig43.database.data.sync

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime

/**
 * Создает глобальный идентификатор записи для синхронизации между устройствами.
 */
fun defaultSyncId(): String = UUID.randomUUID().toString()

/**
 * Возвращает UTC-версию, которая строго новее [previousVersion].
 *
 * Строгий рост защищает sync от отката системных часов и от изменения payload с
 * той же версией. `LocalDateTime` сохранён ради совместимости текущей Room/YDB
 * схемы, но его значение всегда трактуется как UTC.
 *
 * @param previousVersion последняя известная версия строки или `null` для новой строки.
 * @return текущее UTC-время либо [previousVersion] плюс одна наносекунда.
 */
@OptIn(ExperimentalTime::class)
fun defaultUpdatedAt(previousVersion: LocalDateTime? = null): LocalDateTime {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    if (previousVersion == null || now > previousVersion) return now
    return (previousVersion.toInstant(TimeZone.UTC) + 1.nanoseconds)
        .toLocalDateTime(TimeZone.UTC)
}
