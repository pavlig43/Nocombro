import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import ru.pavlig43.convention.extension.libs

class LibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugins.androidLibrary.get().pluginId)


            extensions.configure<LibraryExtension> {
                configureAndroid(this)
            }



        }
    }
}
private fun Project.configureAndroid(
    commonExtension: CommonExtension<*,*,*,*,*,*>
){
    commonExtension.apply {
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        lint {
            checkDependencies = true
        }
        defaultConfig {
            minSdk = libs.versions.android.minSdk.get().toInt()
        }
    }
}

