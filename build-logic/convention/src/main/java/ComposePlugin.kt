import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import ru.pavlig43.convention.extension.androidMainDependencies
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.desktopDependencies
import ru.pavlig43.convention.extension.libs

class ComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {

            apply(plugin = libs.plugins.composeCompiler.get().pluginId)
            apply(plugin = libs.plugins.composeMultiplatform.get().pluginId)



            configureComposeKmp()



        }
    }


}

private val Project.compose
    get() = extensions.getByType<ComposeExtension>().dependencies

internal fun Project.configureComposeKmp() {
    androidMainDependencies {
        implementation(libs.compose.ui.tooling.preview)

    }
    commonMainDependencies {
        implementation(libs.compose.runtime)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.ui)
        implementation(libs.compose.components.resources)

        implementation(libs.androidx.lifecycle.runtime.compose)


        implementation(libs.compose.ui.tooling)
        implementation(libs.compose.ui.tooling.preview)
    }
    desktopDependencies {
        implementation(compose.desktop.currentOs)
    }




    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            optIn.addAll(
                setOf(
                    "androidx.compose.ui.ExperimentalComposeUiApi",
                    "androidx.compose.ui.text.ExperimentalTextApi",
                    "androidx.compose.material.ExperimentalMaterialApi",
                    "androidx.compose.material3.ExperimentalMaterial3Api",
                    "androidx.compose.animation.ExperimentalAnimationApi",
                    "androidx.compose.foundation.ExperimentalFoundationApi",
                    "androidx.compose.foundation.layout.ExperimentalLayoutApi"
                )
            )
        }
    }
}


