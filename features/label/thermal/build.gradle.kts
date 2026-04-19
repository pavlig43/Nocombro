import ru.pavlig43.convention.extension.desktopDependencies

plugins {
    alias(libs.plugins.pavlig43.feature)
}

kotlin {
    desktopDependencies {
        implementation(projects.database)
        implementation(projects.datetime)
        implementation(libs.filekit.dialogs)
        implementation(libs.apache.poi.ooxml)
    }
}
