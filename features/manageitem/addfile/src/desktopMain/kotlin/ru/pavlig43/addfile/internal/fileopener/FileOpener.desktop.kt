package ru.pavlig43.addfile.internal.fileopener

import java.awt.Desktop
import java.io.File

actual class FileOpener {
    actual fun openFile(filePath: String) {
        File(filePath).takeIf { it.exists() }?.let {

            Desktop.getDesktop().open(it)
        }
    }
}