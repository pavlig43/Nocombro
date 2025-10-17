import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.libs

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