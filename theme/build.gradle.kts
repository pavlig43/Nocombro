import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
    alias(libs.plugins.pavlig43.library)
    alias(libs.plugins.pavlig43.compose)

//    alias(libs.plugins.composeMultiplatform)
//    alias(libs.plugins.composeCompiler)


}

android{
    namespace = "ru.pavlig43.theme"
}
//kotlin{
//    commonMainDependencies {
//        implementation(compose.runtime)
//        implementation(compose.foundation)
//        implementation(compose.material3)
//        implementation(compose.components.resources)
//
//    }
//}
compose.resources {
    publicResClass = true
    packageOfResClass = "ru.pavlig43.theme"
    generateResClass = auto
}