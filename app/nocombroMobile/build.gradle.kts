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
    implementation(projects.datetime)
    implementation(libs.kotlinx.serialization.json)
}
