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

}

roomSchemaDocs {
    schemaDirectory("$projectDir/schemas")
    outputDirectory("$projectDir/schemas-docs")
}

