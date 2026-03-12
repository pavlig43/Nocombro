import ru.pavlig43.convention.extension.desktopDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)
  alias(libs.plugins.pavlig43.table)
}

kotlin {
    desktopDependencies {
        implementation(projects.features.table.core)
        implementation(projects.features.table.immutable)
        implementation(projects.core)
        implementation(projects.coreui)
        implementation(projects.theme)
    }
}
