plugins {
    alias(libs.plugins.pavlig43.androidapp)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "ru.pavlig43.nocombro.mobile"

    defaultConfig {
        applicationId = "ru.pavlig43.nocombro.mobile"
    }
}

dependencies {
    implementation(projects.core)
    implementation(projects.corekoin)
    implementation(projects.datetime)
    implementation(projects.theme)
    implementation(libs.filekit.dialogs.compose)
    implementation(libs.kotlinx.serialization.json)
}
