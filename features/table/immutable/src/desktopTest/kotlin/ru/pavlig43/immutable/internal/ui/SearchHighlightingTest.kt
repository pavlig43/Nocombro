package ru.pavlig43.immutable.internal.ui

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly

class SearchHighlightingTest : FunSpec({
    test("finds one match without case sensitivity") {
        findSearchMatches("Test value", "test") shouldContainExactly listOf(0..3)
    }

    test("finds every non-overlapping match") {
        findSearchMatches("AB ab aB", "ab") shouldContainExactly listOf(0..1, 3..4, 6..7)
    }

    test("returns no matches for an empty query") {
        findSearchMatches("value", "   ") shouldContainExactly emptyList()
    }
})
