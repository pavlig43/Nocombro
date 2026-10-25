import ru.pavlig43.convention.extension.desktopDependencies

plugins {

    alias(libs.plugins.pavlig43.kmplibrary)

    alias(libs.plugins.pavlig43.room)
    alias(libs.plugins.pavlig43.koin)
    alias(libs.plugins.pavlig43.serialization)
    alias(libs.plugins.pavlig43.coroutines)
    alias(libs.plugins.pavlig43.testing)
    alias(libs.plugins.roomSchemaDocs)



}



kotlin{
    desktopDependencies {
        implementation(projects.core)
        implementation(projects.datetime)
        api(libs.androidx.room.runtime)
        api(libs.androidx.sqlite.bundled)
        implementation(libs.ydb.jdbc)
        implementation(libs.aws.s3)
        implementation(libs.aws.url.connection.client)
    }

    sourceSets {
        desktopTest {
            dependencies {
                implementation(projects.databaseKit)
            }
        }
    }

}

roomSchemaDocs {
    schemaDirectory("$projectDir/schemas")
    outputDirectory("$projectDir/schemas-docs")
}

// Служебный запуск на согласованной копии SQLite; не запускает приложение.
val reportDesktopCompilation = kotlin.targets.getByName("desktop").compilations.getByName("main")
tasks.register<JavaExec>("syncAnalysisReport") {
    group = "verification"
    description = "Create Markdown and HTML from a SQLite backup and one read-only YDB snapshot"
    dependsOn(reportDesktopCompilation.compileTaskProvider)
    classpath(reportDesktopCompilation.output.allOutputs, reportDesktopCompilation.runtimeDependencyFiles)
    mainClass.set("ru.pavlig43.database.data.sync.SyncAnalysisReportMainKt")
    doFirst {
        args(providers.gradleProperty("syncReportDb").get(), providers.gradleProperty("syncReportDirectory").get())
    }
}

