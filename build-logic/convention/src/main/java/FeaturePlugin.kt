import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class FeaturePlugin:Plugin<Project> {
    override fun apply(target: Project) {
        with(target){
            apply(plugin = libs.plugins.pavlig43.library.get().pluginId)
            apply(plugin = libs.plugins.pavlig43.kmp.get().pluginId)
            apply(plugin = libs.plugins.pavlig43.serialization.get().pluginId)
            apply(plugin = libs.plugins.pavlig43.coroutines.get().pluginId)
            apply(plugin = libs.plugins.pavlig43.compose.get().pluginId)
            apply(plugin = libs.plugins.pavlig43.decompose.get().pluginId)

            commonMainDependencies {
                implementation(project(":core"))
                implementation(project(":corekoin"))
                implementation(project(":theme"))


            }
        }
    }
}

