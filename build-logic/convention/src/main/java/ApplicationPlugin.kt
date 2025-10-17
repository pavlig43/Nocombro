import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import ru.pavlig43.convention.extension.configureApplication
import ru.pavlig43.convention.extension.iosConfig
import ru.pavlig43.convention.extension.libs

class ApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugins.androidApplication.get().pluginId)
            apply(plugin = libs.plugins.pavlig43.kmp.get().pluginId)



            iosConfig {
                baseName = "${this@with.name}App"
                isStatic = true
            }
            configureApplication()





        }

    }
}