package ru.pavlig43.mutable.api.singleLine.component

import androidx.compose.foundation.text.input.TextFieldState

/** Editable buffers owned by a form, observed for validation and change notifications. */
interface SingleLineInputState {
    val textFields: List<TextFieldState>
    val inputErrors: List<String> get() = emptyList()
}
