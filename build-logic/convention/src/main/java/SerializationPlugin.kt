import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.configureTargets
import ru.pavlig43.convention.extension.implementation
import ru.pavlig43.convention.extension.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class SerializationPlugin:Plugin<Project> {
    override fun apply(target: Project) {
        with(target){
            apply(plugin = libs.plugins.kotlinx.serialization.get().pluginId)


            commonMainDependencies  {
                implementation(libs.kotlinx.serialization.json)
            }
        }


    }
}