package ru.pavlig43.files.internal.ui

import androidx.compose.ui.platform.Clipboard

internal expect suspend fun Clipboard.copyTextToClipBoard(text: String)