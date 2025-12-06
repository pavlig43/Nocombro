import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.pavlig43.convention.extension.androidMainDependencies
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.kotlinMultiplatformConfig
import ru.pavlig43.convention.extension.libs

class KoinPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            kotlinMultiplatformConfig {
                commonMainDependencies {
                    api(libs.koin.core)
                    implementation(libs.koin.compose)
                }
                androidMainDependencies {
                    implementation(libs.koin.android)
                }

            }
        }
    }
}

