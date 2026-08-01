package ru.pavlig43.immutable.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import ru.pavlig43.coreui.KeyEventHandler

internal enum class SearchKey {
    Character,
    Backspace,
    Escape,
    Other,
}

internal data class SearchKeyInput(
    val key: SearchKey,
    val character: Char? = null,
    val ctrlPressed: Boolean = false,
    val altPressed: Boolean = false,
    val metaPressed: Boolean = false,
    val rootFocused: Boolean = false,
    val searchFocused: Boolean = false,
)

internal data class SearchKeyResult(
    val query: String,
    val consumed: Boolean,
    val requestSearchFocus: Boolean = false,
    val requestRootFocus: Boolean = false,
)

internal fun applySearchKey(
    query: String,
    input: SearchKeyInput,
): SearchKeyResult {
    if (input.ctrlPressed || input.altPressed || input.metaPressed) {
        return SearchKeyResult(query = query, consumed = false)
    }
    if (input.searchFocused) {
        return if (input.key == SearchKey.Escape) {
            SearchKeyResult(
                query = "",
                consumed = true,
                requestRootFocus = true,
            )
        } else {
            SearchKeyResult(query = query, consumed = false)
        }
    }
    if (!input.rootFocused) {
        return SearchKeyResult(query = query, consumed = false)
    }

    return when (input.key) {
        SearchKey.Character -> {
            val character = input.character
            if (character == null || character.isISOControl()) {
                SearchKeyResult(query = query, consumed = false)
            } else {
                SearchKeyResult(
                    query = query + character,
                    consumed = true,
                    requestSearchFocus = true,
                )
            }
        }

        SearchKey.Backspace -> SearchKeyResult(
            query = query.dropLast(1),
            consumed = query.isNotEmpty(),
            requestSearchFocus = query.isNotEmpty(),
        )

        SearchKey.Escape -> SearchKeyResult(
            query = "",
            consumed = query.isNotEmpty(),
        )

        SearchKey.Other -> SearchKeyResult(query = query, consumed = false)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TableSearchKeyboardHandler(
    owner: Any,
    query: String,
    rootFocused: Boolean,
    searchFocused: Boolean,
    rootFocusRequester: FocusRequester,
    searchFocusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
) {
    val currentQuery = rememberUpdatedState(query)
    val currentRootFocused = rememberUpdatedState(rootFocused)
    val currentSearchFocused = rememberUpdatedState(searchFocused)

    LaunchedEffect(owner) {
        repeat(3) { withFrameNanos { } }
        rootFocusRequester.requestFocus()
    }

    DisposableEffect(owner, rootFocusRequester, searchFocusRequester) {
        val handler: (KeyEvent) -> Boolean = { event ->
            handleSearchKeyEvent(
                event = event,
                query = currentQuery.value,
                rootFocused = currentRootFocused.value,
                searchFocused = currentSearchFocused.value,
                rootFocusRequester = rootFocusRequester,
                searchFocusRequester = searchFocusRequester,
                onQueryChange = onQueryChange,
            )
        }
        KeyEventHandler.subscribe(handler)
        onDispose { KeyEventHandler.unsubscribe(handler) }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun handleSearchKeyEvent(
    event: KeyEvent,
    query: String,
    rootFocused: Boolean,
    searchFocused: Boolean,
    rootFocusRequester: FocusRequester,
    searchFocusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    val key = when (event.key) {
        Key.Backspace -> SearchKey.Backspace
        Key.Escape -> SearchKey.Escape
        else -> SearchKey.Character
    }
    val result = applySearchKey(
        query = query,
        input = SearchKeyInput(
            key = key,
            character = event.utf16CodePoint.takeIf { it > 0 }?.toChar(),
            ctrlPressed = event.isCtrlPressed,
            altPressed = event.isAltPressed,
            metaPressed = event.isMetaPressed,
            rootFocused = rootFocused,
            searchFocused = searchFocused,
        ),
    )
    if (result.query != query) onQueryChange(result.query)
    if (result.requestSearchFocus) searchFocusRequester.requestFocus()
    if (result.requestRootFocus) rootFocusRequester.requestFocus()
    return result.consumed
}
