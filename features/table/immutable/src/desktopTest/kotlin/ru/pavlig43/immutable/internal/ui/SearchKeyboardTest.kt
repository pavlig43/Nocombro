package ru.pavlig43.immutable.internal.ui

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

@OptIn(InternalComposeUiApi::class)
class SearchKeyboardTest : FunSpec({
    test("transfers focus on typed input rather than the preceding key press") {
        listOf('a', 'я').forEach { character ->
            val pressed = applySearchKeyEvent(
                query = "",
                event = KeyEvent(Key.A, KeyEventType.KeyDown, codePoint = character.code),
                rootFocused = true,
                searchFocused = false,
            )
            pressed shouldBe SearchKeyResult(query = "", consumed = false)

            val typed = applySearchKeyEvent(
                query = pressed.query,
                event = KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = character.code),
                rootFocused = true,
                searchFocused = false,
            )
            typed shouldBe SearchKeyResult(
                query = character.toString(),
                consumed = true,
                requestSearchFocus = true,
            )

            val released = applySearchKeyEvent(
                query = typed.query,
                event = KeyEvent(Key.A, KeyEventType.KeyUp, codePoint = character.code),
                rootFocused = false,
                searchFocused = true,
            )
            released shouldBe SearchKeyResult(query = character.toString(), consumed = false)
        }
    }

    test("leaves typed input in the clicked search field to the text field") {
        applySearchKeyEvent(
            query = "",
            event = KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = 'я'.code),
            rootFocused = false,
            searchFocused = true,
        ) shouldBe SearchKeyResult(query = "", consumed = false)
    }

    test("ignores navigation keys and typed control characters") {
        listOf(
            KeyEvent(Key.DirectionLeft, KeyEventType.KeyDown, codePoint = Char.MAX_VALUE.code),
            KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = '\n'.code),
            KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = '\t'.code),
            KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = '\b'.code),
        ).forEach { event ->
            applySearchKeyEvent("query", event, rootFocused = true, searchFocused = false) shouldBe
                SearchKeyResult(query = "query", consumed = false)
        }
    }

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
