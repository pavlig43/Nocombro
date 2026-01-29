import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
    alias(libs.plugins.pavlig43.kmplibrary)
    
    alias(libs.plugins.pavlig43.compose)
    alias(libs.plugins.pavlig43.decompose)

}


commonMainDependencies {
    implementation(projects.core)
    implementation(projects.theme)
    implementation(libs.datetime.wheel.picker)
}
