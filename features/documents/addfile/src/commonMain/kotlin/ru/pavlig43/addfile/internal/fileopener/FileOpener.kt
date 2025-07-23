package ru.pavlig43.addfile.internal.fileopener

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect class FileOpener {
    fun openFile(filePath: String)
}