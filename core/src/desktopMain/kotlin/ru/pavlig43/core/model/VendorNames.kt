package ru.pavlig43.core.model

fun Iterable<String>.toVendorNamesText(): String =
    map(String::trim)
        .filter(String::isNotEmpty)
        .distinctBy(String::lowercase)
        .sortedBy(String::lowercase)
        .joinToString(", ")
