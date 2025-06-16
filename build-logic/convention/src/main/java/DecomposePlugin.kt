import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.implementation
import ru.pavlig43.convention.extension.libs

class DecomposePlugin:Plugin<Project> {
    override fun apply(target: Project) {
        with(target){
            commonMainDependencies {
                implementation(libs.decompose)
                implementation(libs.decompose.compose)

            }
        }
    }
}