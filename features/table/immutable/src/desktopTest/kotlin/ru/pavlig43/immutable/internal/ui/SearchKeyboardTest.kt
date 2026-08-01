package ru.pavlig43.immutable.internal.ui

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SearchKeyboardTest : FunSpec({
    test("adds an English or Russian first character once from the list focus") {
        listOf('a', 'я').forEach { character ->
            val result = applySearchKey(
                query = "",
                input = SearchKeyInput(
                    key = SearchKey.Character,
                    character = character,
                    rootFocused = true,
                ),
            )
            result.query shouldBe character.toString()
            result.consumed shouldBe true
            result.requestSearchFocus shouldBe true
        }
    }

    test("backspace removes one character and escape clears the query") {
        applySearchKey(
            query = "тес",
            input = SearchKeyInput(key = SearchKey.Backspace, rootFocused = true),
        ).query shouldBe "те"

        applySearchKey(
            query = "test",
            input = SearchKeyInput(key = SearchKey.Escape, rootFocused = true),
        ).query shouldBe ""
    }

    test("does not intercept shortcuts or a different focused control") {
        val inputs = listOf(
            SearchKeyInput(key = SearchKey.Character, character = 'x', rootFocused = true, ctrlPressed = true),
            SearchKeyInput(key = SearchKey.Character, character = 'x', rootFocused = true, altPressed = true),
            SearchKeyInput(key = SearchKey.Character, character = 'x', rootFocused = true, metaPressed = true),
            SearchKeyInput(key = SearchKey.Character, character = 'x'),
        )
        inputs.forEach { input ->
            val result = applySearchKey(query = "query", input = input)
            result.query shouldBe "query"
            result.consumed shouldBe false
        }
    }

    test("leaves normal typing to the focused search field") {
        val result = applySearchKey(
            query = "query",
            input = SearchKeyInput(
                key = SearchKey.Character,
                character = 'x',
                searchFocused = true,
            ),
        )
        result.query shouldBe "query"
        result.consumed shouldBe false
    }

    test("escape clears a focused search and returns focus to the list") {
        val result = applySearchKey(
            query = "query",
            input = SearchKeyInput(
                key = SearchKey.Escape,
                searchFocused = true,
            ),
        )
        result.query shouldBe ""
        result.consumed shouldBe true
        result.requestRootFocus shouldBe true
    }
})
