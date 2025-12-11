package ru.pavlig43.coreui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun HighlightedText(
    text: String,
    searchText: String,
    modifier: Modifier = Modifier
) {
    val background = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    val annotated = remember(text, searchText, background) {
        highlightText(text, searchText, background)
    }
    Text(text = annotated, modifier = modifier)
}

@Composable
fun HighlightedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    searchText: String,
    modifier: Modifier = Modifier
) {
    val background = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    val annotated = remember(value, searchText) {
        highlightText(value, searchText, background)
    }

    TextField(
        value = TextFieldValue(
            annotatedString = annotated,
        ),
        onValueChange = { onValueChange(it.text) },
        modifier = modifier
    )
}

private fun highlightText(
    text: String,
    searchText: String,
    background: Color
): AnnotatedString = buildAnnotatedString {
    append(text)
    if (searchText.isNotBlank()) {
        var startIndex = 0
        while (true) {
            val foundIndex = text.indexOf(searchText, startIndex, ignoreCase = true)
            if (foundIndex == -1) break

            addStyle(
                style = SpanStyle(
                    background = background,
                    textDecoration = TextDecoration.Underline,
                    color = Color.Unspecified
                ),
                start = foundIndex,
                end = foundIndex + searchText.length
            )
            startIndex = foundIndex + searchText.length
        }
    }
}
