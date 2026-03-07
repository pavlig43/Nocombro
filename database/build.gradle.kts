import ru.pavlig43.convention.extension.commonMainDependencies

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
    commonMainDependencies {
        implementation(projects.core)
        implementation(projects.datetime)
    }

}

roomSchemaDocs {
    schemaDirectory("$projectDir/schemas")
    outputDirectory("$projectDir/schemas-docs")
}

