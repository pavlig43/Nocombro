package ru.pavlig43.nocombro.mobile.sync

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime

/**
 * Возвращает UTC-версию для новой правки синхронизируемой строки.
 *
 * Если системные часы идут вперёд, берётся текущее UTC-время. Если часы отстали
 * либо совпали с [previousVersion], функция прибавляет к прошлой версии одну
 * наносекунду. Так `updated_at` и `deleted_at` монотонно растут даже после
 * перевода часов устройства назад.
 *
 * @param previousVersion последняя известная версия строки или `null` для новой строки.
 * @return версия, которая строго новее [previousVersion], если она передана.
 */
@OptIn(ExperimentalTime::class)
fun mobileUpdatedAt(previousVersion: LocalDateTime? = null): LocalDateTime {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    if (previousVersion == null || now > previousVersion) return now
    return (previousVersion.toInstant(TimeZone.UTC) + 1.nanoseconds)
        .toLocalDateTime(TimeZone.UTC)
}
