package ru.pavlig43.immutable.internal.component

internal fun interface SearchMatcher<T> {
    fun matches(item: T, normalizedQuery: String): Boolean
}

internal fun <T> displayedTextSearchMatcher(
    values: (T) -> Iterable<String>,
): SearchMatcher<T> = SearchMatcher { item, normalizedQuery ->
    normalizedQuery.isEmpty() || values(item).any { value ->
        value.contains(normalizedQuery, ignoreCase = true)
    }
}

internal fun <T> Iterable<T>.filterBySearch(
    query: String,
    matcher: SearchMatcher<T>,
): List<T> {
    val normalizedQuery = query.trim()
    return if (normalizedQuery.isEmpty()) {
        toList()
    } else {
        filter { item -> matcher.matches(item, normalizedQuery) }
    }
}
