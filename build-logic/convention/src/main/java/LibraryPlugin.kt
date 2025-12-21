import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.androidLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import ru.pavlig43.convention.extension.configureAndroid
import ru.pavlig43.convention.extension.libs

class LibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugins.androidLibrary.get().pluginId)

            apply(plugin = libs.plugins.pavlig43.kmp.get().pluginId)

            extensions.configure<LibraryExtension> {
                configureAndroid(this)
            }



        }
    }
}

