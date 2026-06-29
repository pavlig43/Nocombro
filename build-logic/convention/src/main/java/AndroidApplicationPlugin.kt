import com.android.build.api.dsl.ApplicationExtension
import com.google.devtools.ksp.gradle.KspExtension
import androidx.room.gradle.RoomExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import ru.pavlig43.convention.extension.libs

class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugins.androidApplication.get().pluginId)
            apply(plugin = libs.plugins.composeCompiler.get().pluginId)
            apply(plugin = libs.plugins.ksp.get().pluginId)
            apply(plugin = libs.plugins.room.get().pluginId)

            extensions.configure<KspExtension> {
                arg("room.generateKotlin", "true")
            }

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            extensions.configure<ApplicationExtension> {
                val projectJavaVersion = JavaVersion.toVersion(libs.versions.java.get())

                compileSdk = libs.versions.android.compileSdk.get().toInt()

                defaultConfig {
                    minSdk = libs.versions.android.minSdk.get().toInt()
                    targetSdk = libs.versions.android.targetSdk.get().toInt()
                    versionCode = libs.versions.versionCode.get().toInt()
                    versionName = libs.versions.versionName.get()
                }

                buildFeatures {
                    compose = true
                }

                compileOptions {
                    sourceCompatibility = projectJavaVersion
                    targetCompatibility = projectJavaVersion
                }
            }

            dependencies {
                add("implementation", libs.androidx.activity.compose)
                add("implementation", libs.androidx.core.ktx)
                add("implementation", libs.decompose)
                add("implementation", libs.decompose.compose)
                add("implementation", libs.androidx.room.runtime)
                add("implementation", libs.androidx.sqlite.bundled)
                add("implementation", libs.compose.foundation)
                add("implementation", libs.compose.material3)
                add("implementation", libs.compose.runtime)
                add("implementation", libs.compose.ui)
                add("implementation", libs.compose.ui.tooling.preview)
                add("implementation", libs.kotlinx.coroutines.android)
                add("implementation", libs.kotlinx.datetime)
                add("implementation", libs.koin.android)
                add("debugImplementation", libs.compose.ui.tooling)
                add("ksp", libs.androidx.room.compiler)
            }
        }
    }
}
