import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import ru.pavlig43.convention.extension.androidConfig
import ru.pavlig43.convention.extension.configureComposeKmp
import ru.pavlig43.convention.extension.libs

class ComposePlugin:Plugin<Project> {
    override fun apply(target: Project) {
       with(target){

//           configureTargets()
           apply(plugin = libs.plugins.composeCompiler.get().pluginId)
           apply(plugin = libs.plugins.composeMultiplatform.get().pluginId)

           androidConfig {
               buildFeatures{
                   compose = true
               }
           }
           configureComposeKmp()

       }
    }

}