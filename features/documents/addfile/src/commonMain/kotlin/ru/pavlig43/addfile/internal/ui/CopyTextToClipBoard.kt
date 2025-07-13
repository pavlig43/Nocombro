package ru.pavlig43.addfile.internal.ui

import androidx.compose.ui.platform.Clipboard

internal expect suspend fun Clipboard.copyTextToClipBoard(text: String)