import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.libs

class TablePlugin: Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            commonMainDependencies {
                implementation(libs.wwind.table.core)
                implementation(libs.wwind.table.format)
                implementation(libs.collections.immutable)
            }


        }
    }
}