import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.libs

class KtorPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            commonMainDependencies {

                implementation(libs.ktor.core)
                implementation(libs.ktor.client.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.content.negotiation)

            }
        }
    }
}