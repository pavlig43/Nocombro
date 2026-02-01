import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import ru.pavlig43.convention.extension.configTargetWithoutAndroid
import ru.pavlig43.convention.extension.kotlinMultiplatformConfig
import ru.pavlig43.convention.extension.libs

/**
 * Plugin for Desktop application (Kotlin Multiplatform).
 * For Android, use the new 'application' module with standard Android Application plugin.
 *
 * Note: Desktop-specific configuration (nativeDistributions, etc.) should be done
 * directly in the module's build.gradle.kts file.
 */
class ApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Apply Kotlin Multiplatform plugin
            apply(plugin = libs.plugins.kotlinMultiplatform.get().pluginId)

            // Configure targets without Android
            configTargetWithoutAndroid()

            // Apply Compose plugin
            apply(plugin = libs.plugins.pavlig43.compose.get().pluginId)

            // Configure Native targets (iOS, etc.)
            kotlinMultiplatformConfig {
                targets
                    .filterIsInstance<KotlinNativeTarget>()
                    .forEach { nativeTarget ->
                        nativeTarget.binaries.framework {
                            baseName = "${this@with.name}App"
                            isStatic = true
                        }
                    }
            }

        }
    }
}

