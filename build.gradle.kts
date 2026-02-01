plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dokka)


    //    build-logic
    alias(libs.plugins.pavlig43.application) apply false
    alias(libs.plugins.pavlig43.library) apply false
    alias(libs.plugins.pavlig43.kmplibrary) apply false
    alias(libs.plugins.pavlig43.serialization) apply false
    alias(libs.plugins.pavlig43.coroutines) apply false
    alias(libs.plugins.pavlig43.koin) apply false
    alias(libs.plugins.pavlig43.ktor) apply false
    alias(libs.plugins.pavlig43.feature) apply false
    alias(libs.plugins.pavlig43.sqldelight) apply false
    alias(libs.plugins.pavlig43.decompose) apply false
    alias(libs.plugins.pavlig43.detekt) apply false
    alias(libs.plugins.kotlin.android) apply false


}

    dokka {
        dokkaPublications.html {
            moduleName.set(project.name)
            moduleVersion.set(project.version.toString())
            // Standard output directory for HTML documentation
            outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
            failOnWarning.set(false)
            suppressInheritedMembers.set(false)
            suppressObviousFunctions.set(true)
            offlineMode.set(false)
            includes.from("packages.md", "extra.md")

            // Output directory for additional files
            // Use this block instead of the standard when you
            // want to change the output directory and include extra files
            outputDirectory.set(rootDir.resolve("docs/api/0.x"))

            // Use fileTree to add multiple files
            includes.from(
                fileTree("docs") {
                    include("**/*.md")
                }
            )
        }


    }






subprojects {
    if (!path.contains("sampletable")) {
        apply(plugin = "pavlig43.detekt")
        tasks.register("detektAll") {
            group = "custom"
            val detektTasks = listOf(
                "detektAndroidDebug",
                "detektDesktopMain",
                "detektMetadataCommonMain",
                "detektMetadataMain"
            ).mapNotNull { tasks.findByName(it) }
            dependsOn(detektTasks)
        }
    }
}

tasks.register("createKmpLib") {
    group = "custom"
    doLast {
        val moduleName = findProperty("moduleName")?.toString()
            ?: run {
                println("Имя модуля?")
                readlnOrNull()?.takeIf { it.isNotBlank() }
            }
            ?: error("moduleName not found in gradle.properties for task:createKmpLib- show in root build.gradle.kts")
        val packageName = "ru.pavlig43.$moduleName"
        val moduleDir = file("features/$moduleName")

        listOf(
            "commonMain",
            "androidMain",
            "desktopMain",
        ).forEach { folder ->
            val kmpDir = "src/$folder/kotlin/${packageName.replace(".", "/")}"
            val dirName = File(moduleDir, kmpDir)
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
        val settingsFile = project.rootProject.file("settings.gradle.kts")
        if (settingsFile.exists()) {
            settingsFile.appendText("\ninclude(\":features:$moduleName\")")
        }
    }
}