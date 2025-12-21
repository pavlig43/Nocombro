import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.androidLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import ru.pavlig43.convention.extension.configureAndroid
import ru.pavlig43.convention.extension.libs
import ru.pavlig43.convention.extension.projectJavaVersion

class KmpLibrary: Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugins.pavlig43.kmp.get().pluginId)
            apply(plugin = libs.plugins.android.kotlin.multiplatform.library .get().pluginId)

            extensions.configure<KotlinMultiplatformExtension> {
//                androidLibrary {
//                    compileSdk = libs.versions.android.compileSdk.get().toInt()
//                    minSdk = libs.versions.android.minSdk.get().toInt()
//                    lint {
//                        checkDependencies = true
//                    }
//                }
                androidTarget {
                    compilerOptions {
                        jvmTarget.set(jvmTarget)
                    }

                    }
            }

            extensions.configure<LibraryExtension> {
                configureAndroid(this)
            }



        }
    }
}