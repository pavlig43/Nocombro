package ru.pavlig43.nocombro.mobile.sync

import android.util.Log

private const val MOBILE_SYNC_TAG = "NocombroMobileSync"

/**
 * Дополняет техническую причину сбоя безопасными данными одной строки синхронизации.
 *
 * В [message] можно класть имя таблицы и `sync_id`, но не токены, ключи доступа,
 * URL с секретами или полный ответ удалённого сервиса. Исходная [cause] остаётся
 * доступной для журнала Android (logcat) и не выводится в интерфейс напрямую.
 */
internal class MobileSyncOperationException(
    message: String,
    cause: Throwable,
) : IllegalStateException(message, cause)

/**
 * Пишет полный сбой в журнал Android (logcat) и возвращает короткий текст для интерфейса.
 *
 * Интерфейс не получает SQL, URL или цепочку причин. Полный сбой остаётся
 * только в журнале Android.
 *
 * @param fallback безопасное описание этапа без секретов конфигурации.
 */
internal fun Throwable.mobileSyncErrorMessage(fallback: String): String {
    Log.e(MOBILE_SYNC_TAG, fallback, this)
    return mobileSyncDisplayMessage(fallback)
}

/** Возвращает безопасный текст без SQL, URL и служебных кодов JDBC. */
internal fun Throwable.mobileSyncDisplayMessage(fallback: String): String {
    if (isMobileYdbResourceExhausted()) {
        return "$fallback: YDB временно перегружена. Повторите позже."
    }
    return fallback
}

/** Ищет код временного отказа во всей цепочке JDBC-ошибок. */
internal fun Throwable.isMobileYdbResourceExhausted(): Boolean {
    return causeChain().any { cause ->
        val message = cause.message.orEmpty()
        message.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
            message.contains("ResourceExhausted", ignoreCase = true) ||
            message.contains("CLIENT_RESOURCE_EXHAUSTED", ignoreCase = true) ||
            message.contains("401020")
    }
}

private fun Throwable.causeChain(): Sequence<Throwable> = sequence {
    val seen = mutableSetOf<Throwable>()
    var current: Throwable? = this@causeChain
    while (current != null && seen.add(current)) {
        yield(current)
        current = current.cause
    }
}
