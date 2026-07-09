package ru.pavlig43.nocombro.mobile.sync

import android.util.Log

private const val MOBILE_SYNC_TAG = "NocombroMobileSync"

/**
 * Пишет полный сбой в logcat и возвращает короткий текст для UI.
 */
internal fun Throwable.mobileSyncErrorMessage(fallback: String): String {
    Log.e(MOBILE_SYNC_TAG, fallback, this)

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
