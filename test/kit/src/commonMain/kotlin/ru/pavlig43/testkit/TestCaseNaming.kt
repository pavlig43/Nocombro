package ru.pavlig43.testkit

fun scenario(given: String, whenAction: String, thenResult: String): String {
    return "given $given, when $whenAction, then $thenResult"
}
