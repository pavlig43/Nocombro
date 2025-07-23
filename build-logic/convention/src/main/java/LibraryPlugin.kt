import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import ru.pavlig43.convention.extension.androidMainDependencies
import ru.pavlig43.convention.extension.configureAndroid
import ru.pavlig43.convention.extension.libs

class LibraryPlugin:Plugin<Project> {
    override fun apply(target: Project) {
        with(target){
            apply(plugin = libs.plugins.androidLibrary.get().pluginId)
            apply(plugin = libs.plugins.pavlig43.kmp.get().pluginId)



            configureAndroid()
            androidMainDependencies {
                implementation(libs.androidx.core.ktx)
            }

        }
    }
}