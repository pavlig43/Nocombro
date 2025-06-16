import ru.pavlig43.convention.extension.commonMainDependencies

plugins {

    alias(libs.plugins.pavlig43.library)
    alias(libs.plugins.pavlig43.kmp)
    alias(libs.plugins.pavlig43.room)
    alias(libs.plugins.pavlig43.koin)
    alias(libs.plugins.pavlig43.serialization)
    alias(libs.plugins.pavlig43.coroutines)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

}



android {
    namespace = "ru.pavlig43.database"
}
kotlin{
    commonMainDependencies {
        implementation(compose.runtime)
        implementation(compose.components.resources)
        implementation(projects.core)
    }
}