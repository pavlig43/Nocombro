package ru.pavlig43.addfile.internal.fileopener

import androidx.compose.runtime.Composable

@Composable
internal actual fun getFileOpener(): FileOpener {
    return FileOpener()
}