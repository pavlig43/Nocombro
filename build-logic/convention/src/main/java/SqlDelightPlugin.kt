import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import ru.pavlig43.convention.extension.configureSqlDelight
import ru.pavlig43.convention.extension.libs

class SqlDelightPlugin:Plugin<Project> {
    override fun apply(target: Project) {
        with(target){
            apply(plugin = libs.plugins.sqldelight.get().pluginId)
            configureSqlDelight()
        }
    }
}