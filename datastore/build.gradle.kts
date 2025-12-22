import ru.pavlig43.convention.extension.commonMainDependencies

plugins {

    alias(libs.plugins.pavlig43.kmplibrary)
    
    alias(libs.plugins.pavlig43.koin)
    alias(libs.plugins.pavlig43.coroutines)

}




kotlin{
    commonMainDependencies {
        implementation(libs.datastore)
        implementation(libs.datastore.preferences)
        implementation(projects.core)
    }
}