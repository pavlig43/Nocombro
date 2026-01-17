package ru.pavlig43.files.internal.ui

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import java.awt.datatransfer.StringSelection

internal actual suspend fun Clipboard.copyTextToClipBoard(text: String) {
    setClipEntry(ClipEntry(StringSelection(text)))
}