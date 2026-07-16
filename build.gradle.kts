import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

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
    alias(libs.plugins.versions)
    alias(libs.plugins.roomSchemaDocs) apply false



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

abstract class DetektAllTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val textReports: ConfigurableFileCollection

    @TaskAction
    fun printFindings() {
        val findings = textReports.files
            .filter { it.isFile }
            .flatMap { it.readLines() }
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

        if (findings.isEmpty()) {
            logger.lifecycle("Detekt: no findings.")
        } else {
            logger.warn("Detekt findings: ${findings.size}")
            findings.forEach(logger::warn)
        }
    }
}

/** Общая задача всех проверок Detekt без изменения исходников. */
val detektAll = tasks.register<DetektAllTask>("detektAll") {
    group = "verification"
    description = "Runs every Detekt check registered in the project."
}

/** Общая явная задача автоисправления Detekt во всех Kotlin-модулях. */
val detektAutoFix = tasks.register("detektAutoFix") {
    group = "formatting"
    description = "Runs Detekt auto-correction in every Kotlin module."
}

subprojects {
    if (!path.contains("sampletable")) {
        apply(plugin = "pavlig43.detekt")
        tasks.withType<Detekt>().matching {
            it.name != "detekt" && it.name != "detektAutoFix"
        }.configureEach {
            val detektTask = this
            detektAll.configure {
                dependsOn(detektTask)
                textReports.from(detektTask.txtReportFile)
            }
        }
        detektAutoFix.configure {
            dependsOn(
                tasks.withType<Detekt>().matching {
                    it.name == "detektAutoFix"
                }
            )
        }
    }
}

val smokeCoreDesktopTasks = listOf(
    ":database:desktopTest",
    ":features:analytic:profitability:desktopTest",
    ":features:files:desktopTest",
    ":rootnocombro:desktopTest",
)

val smokeFormsDesktopTasks = listOf(
    ":features:form:transaction:desktopTest",
    ":features:form:product:desktopTest",
    ":features:form:declaration:desktopTest",
    ":features:form:expense:desktopTest",
    ":features:form:vendor:desktopTest",
    ":features:form:document:desktopTest",
)

tasks.register("smokeCoreDesktop") {
    group = "verification"
    description = "Runs desktop smoke tests for database, analytics, files, and root navigation."
    dependsOn(smokeCoreDesktopTasks)
}

tasks.register("smokeFormsDesktop") {
    group = "verification"
    description = "Runs desktop smoke tests for all form modules."
    dependsOn(smokeFormsDesktopTasks)
}

tasks.register("smokeDesktop") {
    group = "verification"
    description = "Runs the full desktop smoke suite."
    dependsOn("smokeCoreDesktop", "smokeFormsDesktop")
}

