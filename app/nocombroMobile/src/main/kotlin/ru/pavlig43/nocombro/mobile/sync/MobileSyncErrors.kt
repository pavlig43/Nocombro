package ru.pavlig43.nocombro.mobile.sync

import android.util.Log

private const val MOBILE_SYNC_TAG = "NocombroMobileSync"

/**
 * Оборачивает техническую причину сбоя безопасным контекстом одной sync-строки.
 *
 * В [message] можно класть имя таблицы и `sync_id`, но не токены, ключи доступа,
 * URL с секретами или полный ответ удалённого сервиса. Исходная [cause] остаётся
 * доступной для logcat и не выводится в интерфейс напрямую.
 */
internal class MobileSyncOperationException(
    message: String,
    cause: Throwable,
) : IllegalStateException(message, cause)

/**
 * Пишет полный сбой в logcat и возвращает короткий текст для UI.
 *
 * Для [MobileSyncOperationException] наружу попадает только заранее очищенный
 * контекст. Для прочих ошибок собираются непустые уникальные сообщения по цепочке
 * причин; цикл в `cause` не приводит к бесконечному обходу.
 *
 * @param fallback безопасное описание этапа без секретов конфигурации.
 */
internal fun Throwable.mobileSyncErrorMessage(fallback: String): String {
    Log.e(MOBILE_SYNC_TAG, fallback, this)

    if (this is MobileSyncOperationException) {
        return "$fallback: ${message.orEmpty()}"
    }

    val details = causeChain()
        .mapNotNull { throwable ->
            throwable.message
                ?.trim()
                ?.takeIf(String::isNotEmpty)
        }
        .distinct()
        .joinToString(separator = " | ")

    return if (details.isBlank()) fallback else "$fallback: $details"
}

private fun Throwable.causeChain(): Sequence<Throwable> = sequence {
    val seen = mutableSetOf<Throwable>()
    var current: Throwable? = this@causeChain
    while (current != null && seen.add(current)) {
        yield(current)
        current = current.cause
    }
}
