import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.kotlinMultiplatformConfig
import ru.pavlig43.convention.extension.libs


class KmpLibrary : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugins.kotlinMultiplatform.get().pluginId)
//             apply(plugin = libs.plugins.android.kotlin.multiplatform.library.get().pluginId)  // Experiment: remove Android plugin
            apply(plugin = libs.plugins.dokka.get().pluginId)


            commonMainDependencies {
                implementation(libs.kotlinx.datetime)
            }


            kotlinMultiplatformConfig {
                jvm("desktop")
//                extensions.findByType(
//                    KotlinMultiplatformAndroidLibraryTarget::class.java
//                )?.apply {
//                    namespace = "ru.pavlig43${project.path.replace(":", ".")}"
//                    androidResources.enable = true
//
//
//                    compileSdk = libs.versions.android.compileSdk.get().toInt()
//                    minSdk = libs.versions.android.minSdk.get().toInt()
//                    lint {
//                        checkDependencies = true
//                    }
//                }

            }

            // Enable context parameters (Kotlin 2.3+ feature) for all targets
            kotlinMultiplatformConfig {
                targets.configureEach {
                    compilations.configureEach {
                        compileTaskProvider.configure {
                            compilerOptions {
                                freeCompilerArgs.add("-Xcontext-parameters")
                            }
                        }
                    }
                }
            }

        }
    }
}