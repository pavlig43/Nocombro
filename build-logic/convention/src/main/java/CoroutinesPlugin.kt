import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.pavlig43.convention.extension.androidMainDependencies
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.commonTestDependencies
import ru.pavlig43.convention.extension.desktopDependencies
import ru.pavlig43.convention.extension.libs

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