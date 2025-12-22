import com.android.build.api.dsl.androidLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.configTargetWithoutAndroid
import ru.pavlig43.convention.extension.kotlinMultiplatformConfig
import ru.pavlig43.convention.extension.libs


class KmpLibrary : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugins.kotlinMultiplatform.get().pluginId)
            apply(plugin = libs.plugins.android.kotlin.multiplatform.library.get().pluginId)

            commonMainDependencies {
                implementation(libs.kotlinx.datetime)
            }

            configTargetWithoutAndroid()

            kotlinMultiplatformConfig {
                @Suppress("UnstableApiUsage")
                androidLibrary {
                    namespace = "ru.pavlig43${project.path.replace(":", ".")}"


                    compileSdk = libs.versions.android.compileSdk.get().toInt()
                    minSdk = libs.versions.android.minSdk.get().toInt()
                    lint {
                        checkDependencies = true
                    }

                }
            }

        }
    }
}