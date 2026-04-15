package ru.pavlig43.testkit

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

fun createTempPath(prefix: String = "nocombro-test-"): Path = Files.createTempDirectory(prefix)

@OptIn(ExperimentalPathApi::class)
fun deleteTempPath(path: Path) {
    path.deleteRecursively()
}
