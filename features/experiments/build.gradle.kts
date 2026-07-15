import ru.pavlig43.convention.extension.desktopDependencies

plugins {
    alias(libs.plugins.pavlig43.feature)
    alias(libs.plugins.pavlig43.testing)
}

kotlin {
    desktopDependencies {
        implementation(projects.core)
        implementation(projects.coreui)
        implementation(projects.database)
        implementation(projects.datetime)
        implementation(projects.features.files)
        implementation(projects.theme)
    }

    sourceSets {
        desktopTest {
            dependencies {
                implementation(projects.databaseKit)
            }
        }
    }
}
