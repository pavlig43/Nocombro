package ru.pavlig43.testkit

import kotlinx.coroutines.delay

suspend fun waitUntil(
    attempts: Int = 50,
    delayMillis: Long = 20,
    predicate: () -> Boolean,
) {
    repeat(attempts) {
        if (predicate()) return
        delay(delayMillis)
    }
    error("Condition was not met in time.")
}

suspend fun <T> waitUntilNotNull(
    attempts: Int = 50,
    delayMillis: Long = 20,
    block: () -> T?,
): T {
    repeat(attempts) {
        block()?.let { return it }
        delay(delayMillis)
    }
    error("Value was not produced in time.")
}
