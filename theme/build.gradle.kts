import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
    alias(libs.plugins.pavlig43.kmplibrary)
    alias(libs.plugins.pavlig43.compose)

}
//kotlin {
//    commonMainDependencies {
//        api(compose.components.resources)
//    }
//}

extensions.getByType<ComposeExtension>().resources {
    publicResClass = true
    packageOfResClass = "ru.pavlig43.theme"
    generateResClass = auto
}