import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)
  alias(libs.plugins.pavlig43.table)
}

kotlin {
    commonMainDependencies {
        implementation(projects.features.table.core)
        implementation(projects.features.table.immutable)
        implementation(projects.core)
        implementation(projects.coreui)
        implementation(projects.theme)
    }
}
