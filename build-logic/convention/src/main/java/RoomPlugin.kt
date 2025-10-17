import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import ru.pavlig43.convention.extension.configureRoomKmp
import ru.pavlig43.convention.extension.libs

class RoomPlugin:Plugin<Project> {
    override fun apply(target: Project) {
        with(target){



            apply(plugin = libs.plugins.room.get().pluginId)
            apply(plugin = libs.plugins.ksp.get().pluginId)


            configureRoomKmp()
        }
    }
}