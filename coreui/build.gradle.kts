import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
    alias(libs.plugins.pavlig43.library)
    alias(libs.plugins.pavlig43.kmp)
    alias(libs.plugins.pavlig43.compose)
    alias(libs.plugins.pavlig43.decompose)

}

android {
    namespace = "ru.pavlig43.coreui"
}
commonMainDependencies {
    implementation(projects.core)
    implementation(libs.datetime.wheel.picker)
}
