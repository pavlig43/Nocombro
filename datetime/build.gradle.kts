import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.desktopDependencies

plugins {
    alias(libs.plugins.pavlig43.kmplibrary)
    alias(libs.plugins.pavlig43.serialization)
    alias(libs.plugins.pavlig43.compose)
    alias(libs.plugins.pavlig43.decompose)
  }
kotlin{
    desktopDependencies {
        implementation(projects.core)
        implementation(projects.coreui)
        implementation(projects.theme)
        implementation(libs.datetime.wheel.picker)
    }
}

