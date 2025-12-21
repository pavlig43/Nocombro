import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.commonTestDependencies
import ru.pavlig43.convention.extension.configureTargets
import ru.pavlig43.convention.extension.libs

class KmpPlugin:Plugin<Project> {
    override fun apply(target: Project) {
        with(target){
//            this.extensions.extensionsSchema.elements.forEach {
//                println(it.publicType.concreteClass.name.substringAfterLast('.'))
//            }
            apply(plugin = libs.plugins.kotlinMultiplatform.get().pluginId)
            configureTargets()
            commonMainDependencies {
                implementation(libs.kotlinx.datetime)
            }
            commonTestDependencies {
                implementation(libs.kotlin.test)
                implementation(libs.turbine)
                implementation(libs.koin.test)
            }
        }
    }
}