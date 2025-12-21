import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import ru.pavlig43.convention.extension.androidMainDependencies
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.desktopDependencies
import ru.pavlig43.convention.extension.libs

class SqlDelightPlugin:Plugin<Project> {
    override fun apply(target: Project) {
        with(target){
            apply(plugin = libs.plugins.sqldelight.get().pluginId)
            commonMainDependencies {
                implementation(libs.sqldelight.coroutines)
            }
            androidMainDependencies {
                implementation(libs.sqldelight.android)
            }
            desktopDependencies {
                implementation(libs.sqldelight.desktop)
            }
        }
    }
}
