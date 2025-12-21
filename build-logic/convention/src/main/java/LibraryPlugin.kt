import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.androidLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import ru.pavlig43.convention.extension.androidMainDependencies
import ru.pavlig43.convention.extension.configureAndroid
import ru.pavlig43.convention.extension.libs

class LibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
//            apply(plugin = libs.plugins.androidLibrary.get().pluginId)
            apply(plugin = libs.plugins.android.kotlin.multiplatform.library.get().pluginId)
            apply(plugin = libs.plugins.pavlig43.kmp.get().pluginId)



//            configureAndroid()
            extensions.configure<KotlinMultiplatformExtension> {

                (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>(
                    "androidMain"
                ) {
                    compileSdk = libs.versions.android.compileSdk.get().toInt()
                    minSdk = libs.versions.android.minSdk.get().toInt()
                }
            }
            androidMainDependencies {
                implementation(libs.androidx.core.ktx)
            }

        }
    }
}
class LibraryPlugin1 : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
//            apply(plugin = libs.plugins.androidLibrary.get().pluginId)
            apply(plugin = libs.plugins.android.kotlin.multiplatform.library.get().pluginId)
            apply(plugin = libs.plugins.pavlig43.kmp.get().pluginId)



//            configureAndroid()
            extensions.configure<KotlinMultiplatformExtension> {

                (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>(
                    "androidMain"
                ) {
                    compileSdk = libs.versions.android.compileSdk.get().toInt()
                    minSdk = libs.versions.android.minSdk.get().toInt()
                }
            }
            androidMainDependencies {
                implementation(libs.androidx.core.ktx)
            }

        }
    }
}