import ru.pavlig43.convention.extension.commonMainDependencies

plugins {

    alias(libs.plugins.pavlig43.kmplibrary)
    
    alias(libs.plugins.pavlig43.room)
    alias(libs.plugins.pavlig43.koin)
    alias(libs.plugins.pavlig43.serialization)
    alias(libs.plugins.pavlig43.coroutines)


}



kotlin{
    commonMainDependencies {
        implementation(projects.core)
    }

}