import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.pavlig43.convention.extension.*

class CoroutinesPlugin:Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            commonMainDependencies {
                implementation(libs.kotlinx.coroutines.core)
            }

            commonTestDependencies {
                implementation(libs.kotlinx.coroutines.test)
            }

            androidMainDependencies {
                implementation(libs.kotlinx.coroutines.android)
            }

            desktopDependencies  {
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}