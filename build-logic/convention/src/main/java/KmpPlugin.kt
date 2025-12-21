import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.commonTestDependencies
import ru.pavlig43.convention.extension.libs
import ru.pavlig43.convention.extension.projectJavaVersion

class KmpPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {

            apply(plugin = libs.plugins.kotlinMultiplatform.get().pluginId)


            extensions.configure<KotlinMultiplatformExtension> {
                androidTarget{
                    compilerOptions {
                        jvmTarget.set(jvmTarget)
                    }
                }
                jvm("desktop")
            }
            commonMainDependencies {
                implementation(libs.kotlinx.datetime)
            }
            commonTestDependencies {
                implementation(libs.kotlin.test)
                implementation(libs.turbine)
                implementation(libs.koin.test)
            }
        }
    }
}