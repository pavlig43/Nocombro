import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.desktopDependencies

plugins {
    alias(libs.plugins.pavlig43.kmplibrary)
    
    alias(libs.plugins.pavlig43.compose)
    alias(libs.plugins.pavlig43.decompose)

}


desktopDependencies {
    implementation(projects.core)
    implementation(projects.theme)
}
