import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
    alias(libs.plugins.pavlig43.kmplibrary)
    alias(libs.plugins.pavlig43.compose)

}


compose.resources {
    publicResClass = true
    packageOfResClass = "ru.pavlig43.theme"
    generateResClass = auto
}