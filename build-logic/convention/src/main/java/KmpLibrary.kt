import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.apply
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.kotlinMultiplatformConfig
import ru.pavlig43.convention.extension.libs


class KmpLibrary : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugins.kotlinMultiplatform.get().pluginId)
            apply(plugin = libs.plugins.android.kotlin.multiplatform.library.get().pluginId)
            apply(plugin = libs.plugins.dokka.get().pluginId)


            commonMainDependencies {
                implementation(libs.kotlinx.datetime)
            }


            kotlinMultiplatformConfig {
                jvm("desktop")
                extensions.findByType(
                    KotlinMultiplatformAndroidLibraryTarget::class.java
                )?.apply {
                    namespace = androidNamespace()
                    androidResources.enable = true

                    compileSdk = libs.versions.android.compileSdk.get().toInt()
                    minSdk = libs.versions.android.minSdk.get().toInt()
                    lint {
                        checkDependencies = true
                    }
                }

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

private fun Project.androidNamespace(): String {
    val suffix = path
        .split(":")
        .filter { it.isNotBlank() }
        .joinToString(".") { it.asJavaIdentifier() }

    return if (suffix.isBlank()) {
        "ru.pavlig43"
    } else {
        "ru.pavlig43.$suffix"
    }
}

private fun String.asJavaIdentifier(): String {
    val normalized = replace(Regex("[^A-Za-z0-9_]"), "_")

    return if (normalized.firstOrNull()?.let(Character::isJavaIdentifierStart) == true) {
        normalized
    } else {
        "_$normalized"
    }
}
