import ru.pavlig43.convention.extension.commonMainDependencies

plugins {

    alias(libs.plugins.pavlig43.library)
    alias(libs.plugins.pavlig43.kmp)
    alias(libs.plugins.pavlig43.room)
    alias(libs.plugins.pavlig43.koin)
    alias(libs.plugins.pavlig43.serialization)
    alias(libs.plugins.pavlig43.coroutines)


}



android {
    namespace = "ru.pavlig43.database"
}
kotlin{
    commonMainDependencies {
        implementation(projects.core)
        implementation(libs.kotlinx.datetime)
    }

}