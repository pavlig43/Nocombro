import ru.pavlig43.convention.extension.commonMainDependencies

plugins {

    alias(libs.plugins.pavlig43.kmplibrary)

    alias(libs.plugins.pavlig43.room)
    alias(libs.plugins.pavlig43.koin)
    alias(libs.plugins.pavlig43.serialization)
    alias(libs.plugins.pavlig43.coroutines)
    alias(libs.plugins.pavlig43.testing)

    id("jp.ntsk.room-schema-docs")


}



kotlin{
    commonMainDependencies {
        implementation(projects.core)
    }

}

// Configure room-schema-docs plugin
roomSchemaDocs {
    schemaDirectory("$projectDir/schemas")
    outputDirectory("$projectDir/schemas-docs")
}

