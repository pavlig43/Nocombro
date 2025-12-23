package ru.pavlig43.convention.extension.templates

import java.io.File
import java.util.*

fun createKmpLib(moduleName: String){

    val packageName = "ru.pavlig43.${moduleName.lowercase()}"
    val moduleDir = File("features/${moduleName.lowercase()}")
    moduleDir.mkdirs()

    listOf(
        "commonMain",
        "androidMain",
        "desktopMain",
    ).forEach { folder ->
        val kmpDir = "src/$folder/kotlin/${packageName.replace(".", "/")}"
        val dirName = File(moduleDir,kmpDir)
        dirName.mkdirs()
    }

    val buildFile = File(moduleDir, "build.gradle.kts")
    buildFile.writeText(
        """
          plugins {
            alias(libs.plugins.pavlig43.feature)  
            }
          
          android {
              namespace = "$packageName"
          }
        """.trimIndent()
    )

}
internal fun String.toCamelCase(): String {

    return this.substring(0, 1).lowercase() + this.substring(1)
}

internal fun String.toPascalCase(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}