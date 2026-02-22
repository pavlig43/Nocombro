plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
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

    id("jp.ntsk.room-schema-docs") version "1.4.0" apply false


    //    build-logic
    alias(libs.plugins.pavlig43.kmplibrary) apply false
    alias(libs.plugins.pavlig43.serialization) apply false
    alias(libs.plugins.pavlig43.coroutines) apply false
    alias(libs.plugins.pavlig43.koin) apply false
    alias(libs.plugins.pavlig43.ktor) apply false
    alias(libs.plugins.pavlig43.feature) apply false
    alias(libs.plugins.pavlig43.sqldelight) apply false
    alias(libs.plugins.pavlig43.decompose) apply false
    alias(libs.plugins.pavlig43.detekt) apply false



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

