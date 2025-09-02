package ru.pavlig43.addfile.internal.fileopener

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun getFileOpener(): FileOpener {
    val context = LocalContext.current
    return FileOpener(context)
}